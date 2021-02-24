package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.plan.PlanAttribute.fromExpr;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.isAggFunc;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class ToPlanTranslator {
  public static PlanNode translate(ASTNode node) {
    final PlanNode plan = translate0(node);
    PlanNode.resolveUsedAttributes(plan);
    return plan;
  }

  private static PlanNode translate0(ASTNode node) {
    return translate0(node.get(RELATION));
  }

  private static PlanNode translate0(Relation relation) {
    // input
    if (relation.isTable()) return InputNode.make(relation);

    final ASTNode querySpec = locateQuerySpecNode(relation.node());
    final ASTNode query = locateQueryNode(relation.node());
    if (querySpec == null) return InputNode.make(relation); // TODO: UNION operator

    final ASTNode from = querySpec.get(QUERY_SPEC_FROM);
    final ASTNode where = querySpec.get(QUERY_SPEC_WHERE);
    final List<ASTNode> selectItems = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final List<ASTNode> groupBy = querySpec.get(QUERY_SPEC_GROUP_BY);
    final List<ASTNode> orderBy = query.get(QUERY_ORDER_BY);
    final ASTNode limit = query.get(QUERY_LIMIT);
    final ASTNode offset = query.get(QUERY_OFFSET);

    PlanNode prev;
    // source
    prev = translateTableSource(from);
    // filter
    prev = translateFilter(where, prev);
    // projection & aggregation
    prev = translateProj(relation.alias(), selectItems, groupBy, prev);
    // sort
    prev = translateSort(orderBy, prev);
    // limit
    prev = translateLimit(limit, offset, prev);

    if (prev == null) throw new IllegalArgumentException("failed to convert AST to plan");

    return prev;
  }

  private static PlanNode translateTableSource(ASTNode tableSource) {
    if (tableSource == null) return null;

    assert TABLE_SOURCE.isInstance(tableSource);

    if (JOINED_SOURCE.isInstance(tableSource)) {
      // join
      final ASTNode onCondition = tableSource.get(JOINED_ON);
      final JoinType joinType = tableSource.get(JOINED_TYPE);

      final PlanNode op;
      if (joinType.isInner()) op = InnerJoinNode.make(onCondition);
      else if (joinType.isOuter()) op = LeftJoinNode.make(onCondition);
      else return null;

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

  private static PlanNode translateProj(
      String qualification,
      List<ASTNode> selections,
      List<ASTNode> groupKeys,
      PlanNode predecessor) {
    if (predecessor == null) return null;

    final List<PlanAttribute> outAttrs = new ArrayList<>(selections.size());
    for (int i = 0; i < selections.size(); i++) {
      final ASTNode selection = selections.get(i);
      outAttrs.add(fromExpr(qualification, aliasOf(selection, i), selection));
    }

    if (!isEmpty(groupKeys) || selections.stream().anyMatch(ToPlanTranslator::isAggregation)) {
      // Aggregation. The result is Agg(Proj(..)). The projections are all attributes used in group
      // keys and aggregations
      final List<PlanAttribute> projAttrs = new ArrayList<>(selections.size() + groupKeys.size());
      for (ASTNode ref : listJoin(gatherColumnRefs(groupKeys), gatherColumnRefs(selections))) {
        final String name = ref.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN);
        projAttrs.add(fromExpr(qualification, name, ref));
      }

      final ProjNode proj = ProjNode.make(projAttrs);
      final AggNode agg = AggNode.make(outAttrs, groupKeys);

      proj.setPredecessor(0, predecessor);
      agg.setPredecessor(0, proj);

      return agg;

    } else {
      // vanilla projection
      final ProjNode proj = ProjNode.make(outAttrs);
      proj.setPredecessor(0, predecessor);
      return proj;
    }
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
      translateFilter0(expr.get(BINARY_LEFT), filters);
      translateFilter0(expr.get(BINARY_RIGHT), filters);

    } else if (binaryOp == BinaryOp.IN_SUBQUERY) {
      final SubqueryFilterNode filter = SubqueryFilterNode.make(expr.get(BINARY_LEFT));
      filter.predecessors()[1] = translate0(expr.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY));
      filters.add(filter);

    } else filters.add(PlainFilterNode.make(expr));
  }

  private static ASTNode locateQuerySpecNode(ASTNode node) {
    if (QUERY_SPEC.isInstance(node)) return node;
    if (QUERY.isInstance(node)) return locateQuerySpecNode(node.get(QUERY_BODY));
    if (DERIVED_SOURCE.isInstance(node)) return locateQuerySpecNode(node.get(DERIVED_SUBQUERY));
    if (SET_OP.isInstance(node)) return null;
    throw new IllegalArgumentException();
  }

  private static ASTNode locateQueryNode(ASTNode node) {
    if (QUERY.isInstance(node)) return node;
    if (DERIVED_SOURCE.isInstance(node)) return node.get(DERIVED_SUBQUERY);
    else throw new IllegalArgumentException();
  }

  private static boolean isAggregation(ASTNode selectItem) {
    final ASTNode funcName = selectItem.get(SELECT_ITEM_EXPR).get(FUNC_CALL_NAME);
    return funcName != null && funcName.get(NAME_2_0) == null && isAggFunc(funcName.get(NAME_2_1));
  }

  private static String aliasOf(ASTNode selectItem, int idx) {
    final String alias = selectItem.get(SELECT_ITEM_ALIAS);
    if (alias != null) return alias;

    final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
    return COLUMN_REF.isInstance(expr)
        ? expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN)
        : "item" + idx;
  }
}
