package sjtu.ipads.wtune.sqlparser.plan1;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.EXISTS_SUBQUERY_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.QUERY_EXPR_QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_LIMIT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_OFFSET;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_ORDER_BY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_DISTINCT_ON;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_GROUP_BY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_SELECT_ITEMS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_OPTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_SOURCE_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_ON;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.AGGREGATE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.EXISTS;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY_SPEC;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SET_OP;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.InnerJoin;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LeftJoin;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperation;
import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperationOption;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

class PlanBuilder {
  private final ASTNode ast;
  private final Schema schema;

  private PlanBuilder(Schema schema, ASTNode ast) {
    this.ast = ast;
    this.schema = schema;
  }

  public static PlanNode buildPlan(ASTNode ast, Schema schema) {
    final PlanNode plan = new PlanBuilder(requireNonNull(schema), requireNonNull(ast)).build0(ast);
    final PlanContext ctx = PlanContext.build(schema);
    PlanContext.installContext(ctx, plan);
    return plan;
  }

  public static PlanNode buildPlan(ASTNode ast) {
    return buildPlan(ast, ast.context().schema());
  }

  private PlanNode build0(ASTNode ast) {
    if (TABLE_SOURCE.isInstance(ast)) return buildTableSource(ast);
    if (!QUERY.isInstance(ast)) throw failed("invalid query");

    final ASTNode queryBody = ast.get(QUERY_BODY);
    PlanNode node;

    if (QUERY_SPEC.isInstance(queryBody)) {
      final ASTNode tableSource = queryBody.get(QUERY_SPEC_FROM);
      final ASTNode where = queryBody.get(QUERY_SPEC_WHERE);
      final List<ASTNode> selectItems = queryBody.get(QUERY_SPEC_SELECT_ITEMS);
      final List<ASTNode> groups = queryBody.get(QUERY_SPEC_GROUP_BY);
      final ASTNode having = queryBody.get(QUERY_SPEC_HAVING);
      final boolean distinct =
          queryBody.getOr(QUERY_SPEC_DISTINCT, false)
              || queryBody.get(QUERY_SPEC_DISTINCT_ON) != null;

      node = buildTableSource(tableSource);
      node = buildFilters(where, node);
      node = buildProjection(distinct, selectItems, groups, having, node);

    } else if (SET_OP.isInstance(queryBody)) {
      final PlanNode lhs = build0(queryBody.get(SET_OP_LEFT));
      final PlanNode rhs = build0(queryBody.get(SET_OP_RIGHT));
      if (lhs == null || rhs == null) throw failed("invalid set operation " + queryBody);

      final SetOperation operation = queryBody.get(SET_OP_TYPE);
      final boolean distinct = queryBody.get(SET_OP_OPTION) == SetOperationOption.DISTINCT;
      node = new SetOpNodeImpl(operation, distinct);
      node.setPredecessor(0, lhs);
      node.setPredecessor(1, rhs);

    } else throw failed("invalid query body: " + queryBody);

    final List<ASTNode> orders = ast.get(QUERY_ORDER_BY);
    final ASTNode limit = ast.get(QUERY_LIMIT);
    final ASTNode offset = ast.get(QUERY_OFFSET);

    node = buildSort(orders, node);
    node = buildLimit(limit, offset, node);

    return node;
  }

  private PlanNode buildProjection(
      boolean explicitDistinct,
      List<ASTNode> selectItems,
      List<ASTNode> groups,
      ASTNode having,
      PlanNode prev) {
    if (containsAgg(selectItems)) {
      /*
       We translate aggregation as Agg(Proj(..)).
       The inner Proj projects all the attributes used in aggregations, groups and having.
       e.g., SELECT SUM(salary) FROM T GROUP BY dept HAVING MAX(age) > 40
          => Proj[dept,salary,age]

       (Actually such statement is invalid in standard SQL,
        in which all the columns appear in GROUP BY must also appear in selection.
        This is a vendor-extension.)
      */

      // 1. Extract column refs used in selectItems, groups and having
      final List<ASTNode> columnRefs =
          new ArrayList<>(selectItems.size() + (groups == null ? 0 : groups.size()) + 1);
      columnRefs.addAll(gatherColumnRefs(selectItems));
      if (groups != null) columnRefs.addAll(gatherColumnRefs(groups));
      if (having != null) columnRefs.addAll(gatherColumnRefs(having));
      // 2. find there are DISTINCT inside aggregation
      final boolean containsDistinctAggregation =
          selectItems.stream().map(SELECT_ITEM_EXPR::get).anyMatch(AGGREGATE_DISTINCT::isPresent);
      // 3. assign an temporary name for column refs and make select items
      final List<ASTNode> selections = new ArrayList<>(columnRefs.size());
      for (int i = 0, bound = columnRefs.size(); i < bound; i++) {
        final ASTNode selection = ASTNode.node(SELECT_ITEM);
        selection.set(SELECT_ITEM_EXPR, columnRefs.get(i));
        selection.set(SELECT_ITEM_ALIAS, "agg_key_" + i);
        selections.add(selection);
      }
      // 4. build Proj node
      final ProjNode proj = ProjNodeImpl.build(containsDistinctAggregation, selections);
      // 5. build Agg node
      final AggNode agg = AggNodeImpl.build(selectItems, groups, having);
      // 6. assemble
      proj.setPredecessor(0, prev);
      agg.setPredecessor(0, proj);

      return agg;

    } else {
      final ProjNode proj = ProjNodeImpl.build(explicitDistinct, selectItems);
      proj.setPredecessor(0, prev);

      return proj;
    }
  }

  private PlanNode buildTableSource(ASTNode tableSource) {
    if (tableSource == null) return null;
    assert TABLE_SOURCE.isInstance(tableSource);
    switch (tableSource.get(TABLE_SOURCE_KIND)) {
      case SIMPLE_SOURCE:
        return buildSimpleTableSource(tableSource);
      case JOINED_SOURCE:
        return buildJoinedTableSource(tableSource);
      case DERIVED_SOURCE:
        return buildDerivedTableSource(tableSource);
      default:
        throw new IllegalArgumentException();
    }
  }

  private PlanNode buildFilters(ASTNode expr, PlanNode predecessor) {
    if (expr == null) return predecessor;

    final List<FilterNode> filters = buildFilters0(expr, new ArrayList<>(4));
    if (filters.isEmpty()) throw failed("not a filter: " + expr);

    filters.sort(new FilterComparator());

    final FilterNode first = head(filters);

    FilterNode cursor = first;
    for (FilterNode filter : filters.subList(1, filters.size())) {
      cursor.setPredecessor(0, filter);
      cursor = filter;
    }

    cursor.setPredecessor(0, predecessor);

    return first;
  }

  private PlanNode buildSort(List<ASTNode> orders, PlanNode predecessor) {
    if (orders == null || orders.isEmpty()) return predecessor;

    final SortNode sort = SortNodeImpl.build(orders);
    sort.setPredecessor(0, predecessor);

    return sort;
  }

  private PlanNode buildLimit(ASTNode limit, ASTNode offset, PlanNode predecessor) {
    if (limit == null && offset == null) return predecessor;

    final LimitNode node = LimitNodeImpl.build(limit, offset);
    node.setPredecessor(0, predecessor);
    return node;
  }

  private PlanNode buildSimpleTableSource(ASTNode tableSource) {
    final String tableName = tableSource.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE);
    final Table table = schema.table(tableName);
    if (table == null) throw failed("unknown table '" + tableName + "'");
    final String alias = coalesce(tableSource.get(SIMPLE_ALIAS), tableName);

    return new InputNodeImpl(table, alias);
  }

  private PlanNode buildJoinedTableSource(ASTNode tableSource) {
    final PlanNode lhs = build0(tableSource.get(JOINED_LEFT));
    final PlanNode rhs = build0(tableSource.get(JOINED_RIGHT));

    if (lhs == null || rhs == null) return null;

    final JoinType joinType = tableSource.get(JOINED_TYPE);
    final ASTNode condition = tableSource.get(JOINED_ON);

    final JoinNode joinNode =
        JoinNodeImpl.build(joinType.isInner() ? InnerJoin : LeftJoin, condition);

    joinNode.setPredecessor(0, lhs);
    joinNode.setPredecessor(1, rhs);

    return joinNode;
  }

  private PlanNode buildDerivedTableSource(ASTNode tableSource) {
    final String alias = tableSource.get(DERIVED_ALIAS);
    if (alias == null) throw failed("subquery without alias");

    final PlanNode subquery = build0(tableSource.get(DERIVED_SUBQUERY));
    if (subquery == null) return null;

    subquery.values().setQualification(alias);
    return subquery;
  }

  private List<FilterNode> buildFilters0(ASTNode expr, List<FilterNode> filters) {
    final BinaryOp op = expr.get(BINARY_OP);
    if (op == BinaryOp.AND) {
      buildFilters0(expr.get(BINARY_LEFT), filters);
      buildFilters0(expr.get(BINARY_RIGHT), filters);

    } else if (op == BinaryOp.IN_SUBQUERY) {
      final InSubFilter filter = InSubFilterImpl.build(expr.get(BINARY_LEFT));
      final PlanNode subquery = build0(expr.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY));
      filter.setPredecessor(1, subquery);

      filters.add(filter);

    } else if (EXISTS.isInstance(expr)) {
      final ExistsFilter filter = new ExistsFilterImpl();
      final PlanNode subquery = build0(expr.get(EXISTS_SUBQUERY_EXPR).get(QUERY_EXPR_QUERY));
      filter.setPredecessor(1, subquery);

      filters.add(filter);

    } else {
      filters.add(PlainFilterNodeImpl.build(expr));
    }

    return filters;
  }

  private RuntimeException failed(String reason) {
    return new IllegalArgumentException("failed to build plan: [" + reason + "] " + ast);
  }

  private static boolean containsAgg(List<ASTNode> selectItems) {
    return selectItems.stream().map(SELECT_ITEM_EXPR::get).anyMatch(AGGREGATE::isInstance);
  }

  private static class FilterComparator implements Comparator<FilterNode> {
    @Override
    public int compare(FilterNode o1, FilterNode o2) {
      final int typeCmp = o1.type().compareTo(o2.type());
      if (typeCmp != 0) return typeCmp;
      else return o1.refs().toString().compareTo(o2.refs().toString());
    }
  }
}
