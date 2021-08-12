package sjtu.ipads.wtune.sqlparser.plan;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.node;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.QUERY_EXPR_QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_LIMIT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_OFFSET;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_ORDER_BY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_GROUP_BY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_OLAP_OPTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_SELECT_ITEMS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WINDOWS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_ON;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.IN_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SET_OP;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.plan.FilterNode.makePlainFilter;
import static sjtu.ipads.wtune.sqlparser.plan.FilterNode.makeSubqueryFilter;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedOnTree;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.isForcedDistinct;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.isGlobalWildcard;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.locateQueryNode;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.locateQuerySpecNode;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.makeSelectItem;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.util.ASTHelper;

public class ToPlanTranslator {
  public static PlanNode toPlan(ASTNode node) {
    if (!isSupported(node)) return null;
    final PlanNode plan = translate0(node);
    resolveUsedOnTree(plan);
    return plan;
  }

  private static PlanNode translate0(ASTNode node) {
    return translate0(node.get(RELATION));
  }

  private static PlanNode translate0(Relation rel) {
    // input
    if (rel.isTable()) return InputNode.make(rel.node(), rel.table(), rel.alias());

    final ASTNode querySpec = locateQuerySpecNode(rel);
    final ASTNode query = locateQueryNode(rel);
    if (querySpec == null)
      return InputNode.make(rel.node(), rel.table(), rel.alias()); // TODO: UNION operator

    final ASTNode from = querySpec.get(QUERY_SPEC_FROM);
    final ASTNode where = querySpec.get(QUERY_SPEC_WHERE);
    final List<ASTNode> items = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final List<ASTNode> groupBy = querySpec.get(QUERY_SPEC_GROUP_BY);
    final ASTNode having = querySpec.get(QUERY_SPEC_HAVING);
    final List<ASTNode> orderBy = query.get(QUERY_ORDER_BY);
    final ASTNode limit = query.get(QUERY_LIMIT);
    final ASTNode offset = query.get(QUERY_OFFSET);

    PlanNode prev;
    // source
    prev = translateTableSource(from);
    // filter
    prev = translateFilter(where, prev);
    // projection & aggregation
    prev = translateProj(rel.alias(), isForcedDistinct(querySpec), items, groupBy, having, prev);
    // sort
    prev = translateSort(orderBy, prev);
    // limit
    prev = translateLimit(limit, offset, prev);

    if (prev == null) throw new IllegalArgumentException("failed to convert AST to plan");

    return prev;
  }

  private static PlanNode translateProj(
      String qualification,
      boolean explicitDistinct,
      List<ASTNode> selectItems,
      List<ASTNode> grouping,
      ASTNode having,
      PlanNode predecessor) {
    if (predecessor == null) return null;

    final List<ASTNode> selections = expandWildcards(selectItems, predecessor);
    final List<ASTNode> groupKeys = coalesce(grouping, emptyList());

    if (!groupKeys.isEmpty() || selections.stream().anyMatch(ASTHelper::isAggSelection)) {
      // Aggregation. The structure should be Agg(Proj(..)). Proj's definedAttrs are all used
      // attributes in group keys and aggregations
      final List<ASTNode> projSelections = new ArrayList<>(selections.size() + groupKeys.size());
      for (ASTNode ref : listJoin(gatherColumnRefs(groupKeys), gatherColumnRefs(selections))) {
        final ASTNode item = node(SELECT_ITEM);
        ASTContext.manage(item, ref.context()); // `item` is dangling, manually attach the context
        item.set(SELECT_ITEM_EXPR, ref);

        projSelections.add(item);
      }

      final ProjNode proj = ProjNode.make(null, projSelections);
      final AggNode agg = AggNode.make(qualification, selections, groupKeys, having);

      proj.setPredecessor(0, predecessor);
      agg.setPredecessor(0, proj);

      proj.setForcedUnique(selections.stream().anyMatch(ASTHelper::isAggSelectionWithDistinct));

      return agg;

    } else {
      // vanilla projection
      final ProjNode proj = ProjNode.make(qualification, selections);
      proj.setPredecessor(0, predecessor);
      proj.setWildcard(isGlobalWildcard(selectItems));
      proj.setForcedUnique(explicitDistinct);
      return proj;
    }
  }

  private static PlanNode translateTableSource(ASTNode tableSource) {
    if (tableSource == null) return null;

    assert TABLE_SOURCE.isInstance(tableSource);

    if (JOINED_SOURCE.isInstance(tableSource)) {
      // join
      final ASTNode onCondition = tableSource.get(JOINED_ON);
      final JoinType joinType = tableSource.get(JOINED_TYPE);

      final PlanNode op = JoinNode.make(joinType.isInner() ? INNER_JOIN : LEFT_JOIN, onCondition);

      final PlanNode left = translateTableSource(tableSource.get(JOINED_LEFT));
      final PlanNode right = translateTableSource(tableSource.get(JOINED_RIGHT));

      if (joinType.isRight()) {
        op.setPredecessor(0, right);
        op.setPredecessor(1, left);
      } else {
        op.setPredecessor(0, left);
        op.setPredecessor(1, right);
      }

      return op;

    } else return translate0(tableSource);
  }

  private static PlanNode translateFilter(ASTNode expr, PlanNode predecessor) {
    if (predecessor == null) return null;
    if (expr == null) return predecessor;

    final List<FilterNode> filters = new ArrayList<>(4);
    translateFilter0(expr, filters);

    if (filters.isEmpty()) throw new IllegalArgumentException("not a filter");

    filters.sort(Comparator.comparing(PlanNode::type));

    for (int i = 0, bound = filters.size() - 1; i < bound; i++)
      filters.get(i).setPredecessor(0, filters.get(i + 1));
    filters.get(filters.size() - 1).setPredecessor(0, predecessor);

    return filters.get(0);
  }

  private static PlanNode translateSort(List<ASTNode> orderKeys, PlanNode predecessor) {
    if (isEmpty(orderKeys)) return predecessor;
    if (predecessor == null) return null;

    final SortNode sort = SortNode.make(orderKeys);
    sort.setPredecessor(0, predecessor);
    return sort;
  }

  private static PlanNode translateLimit(ASTNode limit, ASTNode offset, PlanNode predecessor) {
    if (limit == null) return predecessor;
    if (predecessor == null) return null;

    final LimitNode limitNode = LimitNode.make(limit, offset);
    limitNode.setPredecessor(0, predecessor);
    return limitNode;
  }

  private static void translateFilter0(ASTNode expr, List<FilterNode> filters) {
    final BinaryOp binaryOp = expr.get(BINARY_OP);

    if (binaryOp == AND) {
      translateFilter0(expr.get(BINARY_RIGHT), filters);
      translateFilter0(expr.get(BINARY_LEFT), filters);

    } else if (binaryOp == IN_SUBQUERY) {
      final FilterNode filter = makeSubqueryFilter(expr.get(BINARY_LEFT));
      final PlanNode subquery = translate0(expr.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY));
      filter.setPredecessor(1, subquery);

      filters.add(filter);

    } else filters.add(makePlainFilter(expr));
  }

  private static List<ASTNode> expandWildcards(List<ASTNode> selectItems, PlanNode predecessor) {
    if (selectItems.stream().map(SELECT_ITEM_EXPR::get).noneMatch(WILDCARD::isInstance))
      return selectItems;

    final List<ASTNode> ret = new ArrayList<>(selectItems.size() << 1);
    for (ASTNode item : selectItems) {
      final ASTNode expr = item.get(SELECT_ITEM_EXPR);
      if (!WILDCARD.isInstance(expr)) {
        ret.add(item);
        continue;
      }

      final ASTNode table = expr.get(WILDCARD_TABLE);
      final String tableName = table != null ? table.get(TABLE_NAME_TABLE) : null;

      for (AttributeDef inAttr : predecessor.definedAttributes())
        if (inAttr.name() != null
            && (tableName == null || tableName.equals(inAttr.qualification()))) {
          final ASTNode expandedItem = makeSelectItem(inAttr.makeColumnRef());
          ASTContext.manage(expandedItem, item.context());

          ret.add(expandedItem);
        }
    }

    return ret;
  }

  private static boolean isSupported(ASTNode node) {
    final Checker checker = new Checker();
    node.accept(checker);
    return checker.passed;
  }

  private static class Checker implements ASTVistor {
    private boolean passed = true;

    @Override
    public boolean enter(ASTNode node) {
      return passed;
    }

    @Override
    public boolean enterColumnRef(ASTNode columnRef) {
      final Attribute attr = columnRef.get(ATTRIBUTE);
      if (attr != null && columnRef.get(RELATION).isForeignAttribute(attr)) {
        passed = false;
        return false;
      }
      return true;
    }

    @Override
    public boolean enterQuery(ASTNode query) {
      if (SET_OP.isInstance(query.get(QUERY_BODY))) {
        passed = false;
        return false;
      } else return true;
    }

    @Override
    public boolean enterQuerySpec(ASTNode querySpec) {
      if (querySpec.get(QUERY_SPEC_WINDOWS) != null
          || querySpec.get(QUERY_SPEC_OLAP_OPTION) != null) {
        passed = false;
        return false;
      }
      return true;
    }

    @Override
    public boolean enterCreateTable(ASTNode createTable) {
      passed = false;
      return false;
    }
  }
}
