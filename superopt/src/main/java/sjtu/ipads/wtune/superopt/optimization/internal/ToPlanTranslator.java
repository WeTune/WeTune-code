package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.superopt.optimization.*;

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
  public Operator translate(ASTNode node) {
    return translate(node.get(RELATION));
  }

  private Operator translate(Relation relation) {
    if (relation.isTable()) return InputOp.make(relation);

    final ASTNode querySpec = querySpecNode(relation.node());

    if (querySpec == null) return InputOp.make(relation); // TODO: UNION operator

    final List<Attribute> attributes = relation.attributes();
    final ProjOp proj = ProjOp.make(attributes);

    final ASTNode where = querySpec.get(QUERY_SPEC_WHERE);
    if (where != null) proj.predecessors()[0] = translateFilter(where);

    final ASTNode from = querySpec.get(QUERY_SPEC_FROM);
    if (from != null) {
      final Operator source = translateTableSource(from);
      if (source == null) return null;
      if (where != null) proj.predecessors()[0].predecessors()[0] = source;
      else proj.predecessors()[0] = source;
    }

    return proj;
  }

  private Operator translateTableSource(ASTNode tableSource) {
    assert TABLE_SOURCE.isInstance(tableSource);

    if (JOINED_SOURCE.isInstance(tableSource)) {
      final ASTNode onCondition = tableSource.get(JOINED_ON);
      final JoinType joinType = tableSource.get(JOINED_TYPE);

      final Operator op;
      if (joinType.isInner()) op = InnerJoinOp.build(onCondition);
      else if (joinType.isOuter()) op = LeftJoinOp.build(onCondition);
      else return null;

      final Operator left = translateTableSource(tableSource.get(JOINED_LEFT));
      final Operator right = translateTableSource(tableSource.get(JOINED_RIGHT));
      if (joinType.isRight()) {
        op.predecessors()[0] = right;
        op.predecessors()[1] = left;
      } else {
        op.predecessors()[0] = left;
        op.predecessors()[1] = right;
      }

      return op;

    } else return translate(tableSource);
  }

  private Operator translateFilter(ASTNode expr) {
    final List<FilterOp> filters = new ArrayList<>(4);
    translateFilter0(expr, filters);
    return FilterGroupOp.make(expr, filters);
  }

  private void translateFilter0(ASTNode expr, List<FilterOp> filters) {
    final BinaryOp binaryOp = expr.get(BINARY_OP);

    if (binaryOp == AND) {
      translateFilter0(expr.get(BINARY_LEFT), filters);
      translateFilter0(expr.get(BINARY_RIGHT), filters);

    } else if (binaryOp == BinaryOp.IN_SUBQUERY) {
      final SubqueryFilterOp filter = SubqueryFilterOp.make(expr);
      filter.predecessors()[1] = translate(expr.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY));
      filters.add(filter);

    } else filters.add(PlainFilterOp.make(expr));
  }

  private ASTNode querySpecNode(ASTNode node) {
    if (QUERY_SPEC.isInstance(node)) return node;
    if (QUERY.isInstance(node)) return querySpecNode(node.get(QUERY_BODY));
    if (DERIVED_SOURCE.isInstance(node)) return querySpecNode(node.get(DERIVED_SUBQUERY));
    if (SET_OP.isInstance(node)) return null;
    throw new IllegalArgumentException();
  }
}
