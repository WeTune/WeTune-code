package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind;
import sjtu.ipads.wtune.sql.schema.Column;

import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.SimpleSource;
import static sjtu.ipads.wtune.sql.ast.constants.UnaryOpKind.NOT;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.gatherColRefs;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;
import static sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport.*;

class JoinGraphBuilder {
  static JoinGraph build(SqlContext ctx) {
    return build(SqlNode.mk(ctx, ctx.root()));
  }

  static JoinGraph build(SqlNode root) {
    final JoinGraph graph = new JoinGraphImpl();

    final SqlNodes tables = nodeLocator().accept(SimpleSource).gather(root);
    for (SqlNode table : tables) graph.addTable(getEnclosingRelation(table));

    final SqlNodes joinConds = nodeLocator().accept(SqlSupport::isColRefEq).gather(root);
    for (SqlNode joinCond : joinConds) addJoinCondition(graph, joinCond);

    final SqlNodes inSubExprs = nodeLocator().accept(JoinGraphBuilder::isInSubExpr).gather(root);
    for (SqlNode inSubExpr : inSubExprs) addInSubquery(graph, inSubExpr);

    return graph;
  }

  private static void addJoinCondition(JoinGraph graph, SqlNode joinCond) {
    if (isNegated(joinCond)) return;

    final Attribute lhsKey = resolveAttribute(joinCond.$(Binary_Left));
    final Attribute rhsKey = resolveAttribute(joinCond.$(Binary_Right));
    if (lhsKey == null || rhsKey == null) return;

    final Column lhsCol = traceRef(lhsKey).column(), rhsCol = traceRef(rhsKey).column();
    if (lhsCol == null || rhsCol == null) return;

    graph.addJoin(traceRef(lhsKey).owner(), lhsCol, traceRef(rhsKey).owner(), rhsCol);
  }

  private static void addInSubquery(JoinGraph graph, SqlNode inSub) {
    if (isNegated(inSub)) return;

    final SqlNodes lhsColRefs = gatherColRefs(inSub.$(Binary_Left));
    if (lhsColRefs.isEmpty()) return;

    final Attribute lhsKey = resolveAttribute(lhsColRefs.get(0));
    if (lhsKey == null) return;

    final Attribute lhsBaseKey = traceRef(lhsKey);
    final Column lhsCol = lhsBaseKey.column();
    if (lhsCol == null) return;

    final Relation subqueryRel = getEnclosingRelation(inSub.$(Binary_Right).$(QueryExpr_Query));
    final Attribute rhsBaseKey = traceRef(subqueryRel.attributes().get(0));
    final Column rhsCol = rhsBaseKey.column();
    if (rhsCol == null) return;

    graph.addJoin(lhsBaseKey.owner(), lhsCol, rhsBaseKey.owner(), rhsCol);
  }

  private static boolean isInSubExpr(SqlNode node) {
    return node.$(Binary_Op) == BinaryOpKind.IN_SUBQUERY;
  }

  private static boolean isNegated(SqlNode node) {
    boolean negated = false;
    while (Expr.isInstance(node)) {
      if (node.$(Unary_Op) == NOT) negated = !negated;
      node = node.parent();
    }
    return negated;
  }
}
