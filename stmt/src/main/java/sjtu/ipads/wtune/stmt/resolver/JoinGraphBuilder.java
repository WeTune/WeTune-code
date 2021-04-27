package sjtu.ipads.wtune.stmt.resolver;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.QUERY_EXPR_QUERY;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;
import static sjtu.ipads.wtune.stmt.resolver.BoolExprManager.BOOL_EXPR;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;

class JoinGraphBuilder implements ASTVistor {
  private final ASTNode node;
  private final JoinGraph graph;

  private JoinGraphBuilder(ASTNode node) {
    this.node = node;
    this.graph = new JoinGraphImpl();
  }

  public static JoinGraph build(ASTNode node) {
    if (node.context().manager(BoolExprManager.class) == null) Resolution.resolveBoolExpr(node);

    return new JoinGraphBuilder(node).build0();
  }

  private JoinGraph build0() {
    node.accept(this);
    return graph;
  }

  @Override
  public void leaveBinary(ASTNode binary) {
    final BoolExpr boolExpr = binary.get(BOOL_EXPR);
    if (boolExpr == null || !boolExpr.isPrimitive()) return;

    final Attribute leftAttr, rightAttr;
    if (binary.get(BINARY_OP) == BinaryOp.IN_SUBQUERY) {

      final ASTNode leftColRef = gatherColumnRefs(binary.get(BINARY_LEFT)).get(0);
      leftAttr = leftColRef.get(ATTRIBUTE).reference(true);

      final Relation subqueryRel = binary.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY).get(RELATION);
      rightAttr = subqueryRel.attributes().get(0).reference(true);

    } else if (boolExpr.isJoinKey()) {
      leftAttr = binary.get(BINARY_LEFT).get(ATTRIBUTE).reference(true);
      rightAttr = binary.get(BINARY_RIGHT).get(ATTRIBUTE).reference(true);

    } else return;
    if (leftAttr == null || rightAttr == null) return;

    final Column leftCol = leftAttr.column(true);
    final Relation leftRel = leftAttr.owner();
    final Column rightCol = rightAttr.column(true);
    final Relation rightRel = rightAttr.owner();

    graph.addJoin(leftRel, leftCol, rightRel, rightCol);
  }

  @Override
  public void leaveSimpleTableSource(ASTNode simpleTableSource) {
    graph.addTable(simpleTableSource.get(RELATION));
  }
}
