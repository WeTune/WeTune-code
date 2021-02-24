package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.plan.PlanAttribute.fromAttrs;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.isAggFunc;

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
    if (querySpec == null) return InputNode.make(relation); // TODO: UNION operator

    PlanNode prev = null;

    // source
    final ASTNode from = querySpec.get(QUERY_SPEC_FROM);
    if (from != null) prev = translateTableSource(from);
    // filter
    final ASTNode where = querySpec.get(QUERY_SPEC_WHERE);
    if (where != null) prev = translateFilter(where, prev);

    // projection
    prev = translateProj(relation, querySpec.get(QUERY_SPEC_GROUP_BY), prev);

    if (prev == null) throw new IllegalArgumentException("failed to convert AST to plan");

    return prev;
  }

  private static PlanNode translateTableSource(ASTNode tableSource) {
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
      Relation relation, List<ASTNode> groupKeys, PlanNode predecessor) {
    if (predecessor == null) return null;

    final boolean isAggregated = isEmpty(groupKeys) || isAggregated(relation);

    if (isAggregated) {
      final List<Attribute> attrs = relation.attributes();

      final ProjNode proj = ProjNode.make(fromAttrs(attributesUsedInAgg(attrs), relation.alias()));
      final AggNode agg = AggNode.make(fromAttrs(attrs, relation.alias()), groupKeys);

      proj.setPredecessor(0, predecessor);
      agg.setPredecessor(0, proj);

      return agg;

    } else {
      final ProjNode proj = ProjNode.make(fromAttrs(relation.attributes(), relation.alias()));
      proj.setPredecessor(0, predecessor);
      return proj;
    }
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

  private static boolean isAggregated(Relation relation) {
    for (Attribute attribute : relation.attributes()) {
      final ASTNode selectItem = attribute.selectItem();
      if (selectItem == null) continue;
      final ASTNode funcName = selectItem.get(SELECT_ITEM_EXPR).get(FUNC_CALL_NAME);
      if (funcName != null && funcName.get(NAME_2_0) == null && isAggFunc(funcName.get(NAME_2_1)))
        return true;
    }
    return false;
  }

  private static List<Attribute> attributesUsedInAgg(List<Attribute> aggs) {
    return aggs.stream()
        .map(Attribute::selectItem)
        .map(ColumnRefCollector::collectColumnRefs)
        .flatMap(Collection::stream)
        .map(it -> it.get(ATTRIBUTE))
        .collect(Collectors.toList());
  }
}
