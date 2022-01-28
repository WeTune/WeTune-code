package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast.ExprKind;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.common.utils.IterableSupport.all;
import static sjtu.ipads.wtune.sql.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sql.SqlSupport.mkBinary;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.Binary;
import static sjtu.ipads.wtune.sql.ast.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Joined_On;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.JoinedSource;
import static sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind.AND;

public abstract class NormalizationSupport {
  private NormalizationSupport() {}

  public static void normalizeAst(SqlNode node) {
    InlineLiteralTable.normalize(node);
    NormalizeRightJoin.normalize(node);
    Clean.clean(node);
    NormalizeGrouping.normalize(node);
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
      if (nodeEquals(node, parent.$(QuerySpec_Where))) parent.remove(QuerySpec_Where);
      else if (nodeEquals(node, parent.$(QuerySpec_Having))) parent.remove(QuerySpec_Having);
      else throw new IllegalArgumentException();
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

  static boolean isConstant(SqlNode node) {
    final ExprKind exprKind = node.$(Expr_Kind);
    if (exprKind == null) return false;
    switch (exprKind) {
      case Literal:
      case Symbol:
        return true;
      case Cast:
        return isConstant(node.$(Cast_Expr));
      case Collate:
        return isConstant(node.$(Collate_Expr));
      case Interval:
        return isConstant(node.$(Interval_Expr));
      case ConvertUsing:
        return isConstant(node.$(ConvertUsing_Expr));
      case Default:
        return isConstant(node.$(Default_Col));
      case Values:
        return isConstant(node.$(Values_Expr));
      case Unary:
        return isConstant(node.$(Unary_Expr));
      case Binary:
        return isConstant(node.$(Binary_Left)) && isConstant(node.$(Binary_Right));
      case Ternary:
        return isConstant(node.$(Ternary_Left))
            && isConstant(node.$(Ternary_Middle))
            && isConstant(node.$(Ternary_Right));
      case Tuple:
        return all(node.$(Tuple_Exprs), NormalizationSupport::isConstant);
      case FuncCall:
        return node.$(FuncCall_Name) != null
            && !node.$(FuncCall_Name).$(Name2_1).contains("rand")
            && all(node.$(FuncCall_Args), NormalizationSupport::isConstant);
      case Match:
        return isConstant(node.$(Match_Expr))
            && all(node.$(Match_Cols), NormalizationSupport::isConstant);
      case Case:
        final SqlNode cond = node.$(Case_Cond);
        final SqlNode else_ = node.$(Case_Else);
        return (cond == null || isConstant(cond))
            && (else_ == null || isConstant(else_))
            && all(node.$(Case_Whens), NormalizationSupport::isConstant);
      case When:
        return isConstant(node.$(When_Cond)) && isConstant(node.$(When_Expr));
    }
    return false;
  }
}
