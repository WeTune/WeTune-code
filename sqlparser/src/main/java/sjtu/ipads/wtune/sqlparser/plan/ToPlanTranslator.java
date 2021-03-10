package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.AGGREGATE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.locateQueryNode;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.locateQuerySpecNode;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class ToPlanTranslator {
  public static PlanNode toPlan(ASTNode node) {
    final PlanNode plan = translate0(node);
    PlanNode.resolveUsedOnTree(plan);
    return plan;
  }

  private static PlanNode translate0(ASTNode node) {
    return translate0(node.get(RELATION));
  }

  private static PlanNode translate0(Relation rel) {
    // input
    if (rel.isTable()) return InputNode.make(rel.table(), rel.alias());

    final ASTNode querySpec = locateQuerySpecNode(rel);
    final ASTNode query = locateQueryNode(rel);
    if (querySpec == null) return InputNode.make(rel.table(), rel.alias()); // TODO: UNION operator

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
    prev = translateProj(rel.alias(), isDistinct(querySpec), selectItems, groupBy, prev);
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
      List<ASTNode> selections,
      List<ASTNode> groupKeys,
      PlanNode predecessor) {
    if (predecessor == null) return null;

    final boolean isWildcard = isWildcard(selections);
    selections = expandWildcards(selections, predecessor);
    groupKeys = groupKeys != null ? groupKeys : Collections.emptyList();

    if (!groupKeys.isEmpty() || selections.stream().anyMatch(ToPlanTranslator::isAggregation)) {
      // Aggregation. The structure should be Agg(Proj(..)). Proj's definedAttrs are all attributes
      // used in group keys and aggregations
      final List<ASTNode> projSelections = new ArrayList<>(selections.size() + groupKeys.size());
      for (ASTNode ref : listJoin(gatherColumnRefs(groupKeys), gatherColumnRefs(selections))) {
        final ASTNode item = ASTNode.node(SELECT_ITEM);
        item.set(SELECT_ITEM_EXPR, ref);
        projSelections.add(item);
      }

      final ProjNode proj = ProjNode.make(null, projSelections);
      final AggNode agg = AggNode.make(qualification, selections, groupKeys);

      proj.setPredecessor(0, predecessor);
      agg.setPredecessor(0, proj);

      proj.setForcedUnique(isDistinctAggregation(selections));

      return agg;

    } else {
      // vanilla projection
      final ProjNode proj = ProjNode.make(qualification, selections);
      proj.setPredecessor(0, predecessor);
      proj.setWildcard(isWildcard);
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

    } else if (binaryOp == BinaryOp.IN_SUBQUERY) {
      final SubqueryFilterNode filter = SubqueryFilterNode.make(expr);
      final PlanNode subquery = translate0(expr.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY));
      filter.setPredecessor(1, subquery);

      filters.add(filter);

    } else filters.add(PlainFilterNode.make(expr));
  }

  private static boolean isDistinct(ASTNode querySpec) {
    return querySpec.getOr(QUERY_SPEC_DISTINCT, false)
        || querySpec.get(QUERY_SPEC_DISTINCT_ON) != null;
  }

  private static boolean isWildcard(List<ASTNode> selectItems) {
    if (selectItems.size() != 1) return false;
    final ASTNode expr = selectItems.get(0).get(SELECT_ITEM_EXPR);
    return WILDCARD.isInstance(expr) && expr.get(WILDCARD_TABLE) == null;
  }

  private static boolean isDistinctAggregation(List<ASTNode> selectItems) {
    return selectItems.stream()
        .anyMatch(it -> it.get(SELECT_ITEM_EXPR).getOr(AGGREGATE_DISTINCT, false));
  }

  private static boolean isAggregation(ASTNode selectItem) {
    return AGGREGATE.isInstance(selectItem.get(SELECT_ITEM_EXPR));
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
        if (tableName == null || tableName.equals(inAttr.qualification()))
          ret.add(inAttr.toSelectItem());
    }

    return ret;
  }
}
