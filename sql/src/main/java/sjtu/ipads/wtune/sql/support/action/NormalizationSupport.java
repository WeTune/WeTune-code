package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.sql.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sql.SqlSupport.mkBinary;
import static sjtu.ipads.wtune.sql.ast.ExprFields.Binary_Left;
import static sjtu.ipads.wtune.sql.ast.ExprFields.Binary_Right;
import static sjtu.ipads.wtune.sql.ast.ExprKind.Binary;
import static sjtu.ipads.wtune.sql.ast.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.QuerySpec_Where;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Joined_On;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.JoinedSource;
import static sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind.AND;

public abstract class NormalizationSupport {
  private NormalizationSupport() {}

  public static void normalizeAst(SqlNode node) {
    InlineLiteralTable.normalize(node);
    Clean.clean(node);
    NormalizeBool.normalize(node);
    NormalizeJoinCond.normalize(node);
    NormalizeTuple.normalize(node);
  }

  public static void installParamMarkers(SqlNode node) {
    InstallParamMarker.normalize(node);
  }

  static void detachExpr(SqlNode node) {
    final SqlContext ctx = node.context();
    final SqlNode parent = node.parent();
    if (QuerySpec.isInstance(parent)) {
      parent.remove(QuerySpec_Where);
      ctx.setParentOf(node.nodeId(), NO_SUCH_NODE);
      return;
    }
    if (JoinedSource.isInstance(parent)) {
      parent.remove(Joined_On);
      ctx.setParentOf(node.nodeId(), NO_SUCH_NODE);
    }
    if (!Binary.isInstance(parent)) return;

    final SqlNode lhs = parent.$(Binary_Left), rhs = parent.$(Binary_Right);
    final SqlNode otherSide = nodeEquals(lhs, node) ? rhs : lhs;
    ctx.displaceNode(parent.nodeId(), otherSide.nodeId());
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
