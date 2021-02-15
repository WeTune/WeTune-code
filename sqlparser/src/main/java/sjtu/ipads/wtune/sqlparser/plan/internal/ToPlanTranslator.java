package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;

public class ToPlanTranslator {
  public static PlanNode translate(ASTNode node) {
    return translate(node.get(RELATION));
  }

  private static PlanNode translate(Relation relation) {
    // input
    if (relation.isTable()) return InputNode.make(relation);

    final ASTNode querySpec = locateQuerySpecNode(relation.node());
    if (querySpec == null) return InputNode.make(relation); // TODO: UNION operator

    // source
    PlanNode source = null;
    final ASTNode from = querySpec.get(QUERY_SPEC_FROM);
    if (from != null) source = translateTableSource(from);
    if (source == null) throw new IllegalArgumentException("null table source is not supported");
    source.resolveUsedAttributes();

    // filter
    PlanNode filter = null;
    final ASTNode where = querySpec.get(QUERY_SPEC_WHERE);
    if (where != null) {
      filter = translateFilter(where);
      filter.setPredecessor(0, source);
      filter.resolveUsedAttributes();
    }

    // projection
    final ProjNode proj = ProjNode.make(relation);
    proj.setPredecessor(0, filter != null ? filter : source);
    proj.resolveUsedAttributes();

    return proj;
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

    } else return translate(tableSource);
  }

  private static PlanNode translateFilter(ASTNode expr) {
    final List<FilterNode> filters = new ArrayList<>(4);
    translateFilter0(expr, filters);
    return FilterGroupNode.make(expr, filters);
  }

  private static void translateFilter0(ASTNode expr, List<FilterNode> filters) {
    final BinaryOp binaryOp = expr.get(BINARY_OP);

    if (binaryOp == AND) {
      translateFilter0(expr.get(BINARY_LEFT), filters);
      translateFilter0(expr.get(BINARY_RIGHT), filters);

    } else if (binaryOp == BinaryOp.IN_SUBQUERY) {
      final SubqueryFilterNode filter = SubqueryFilterNode.make(expr.get(BINARY_LEFT));
      filter.predecessors()[1] = translate(expr.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY));
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
}
