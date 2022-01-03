package sjtu.ipads.wtune.sql.support.normalize;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.sql.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sql.SqlSupport.mkBinary;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.Binary_Left;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.Binary_Right;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.Binary;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.QuerySpec_Where;
import static sjtu.ipads.wtune.sql.ast1.constants.BinaryOpKind.AND;

public abstract class NormalizationSupport {
  private NormalizationSupport() {}

  public static void normalizeAst(SqlNode node) {
    InlineLiteralTable.normalize(node);
    Clean.clean(node);
    NormalizeBool.normalize(node);
    NormalizeJoinCond.normalize(node);
    NormalizeTuple.normalize(node);
  }

  static void detachExpr(SqlNode node) {
    final SqlNode parent = node.parent();
    if (QuerySpec.isInstance(parent)) parent.remove(QuerySpec_Where);
    if (!Binary.isInstance(parent)) return;

    final SqlNode lhs = parent.$(Binary_Left), rhs = parent.$(Binary_Right);
    final SqlNode otherSide = nodeEquals(lhs, node) ? rhs : lhs;
    node.context().displaceNode(parent.nodeId(), otherSide.nodeId());
  }

  static void conjunctExprTo(SqlNode parent, FieldKey<SqlNode> clause, SqlNode expr) {
    final SqlNode cond = parent.$(clause);
    if (cond == null) parent.$(clause, expr);
    else {
      final SqlContext ctx = parent.context();
      final SqlNode lhs = copyAst(cond).go();
      final SqlNode newCond = mkBinary(ctx, AND, lhs, expr);
      ctx.displaceNode(cond.nodeId(), newCond.nodeId());
    }
  }
}
