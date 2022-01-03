package sjtu.ipads.wtune.sql.support.normalize;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast1.ExprKind;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.support.NodeCollector;

import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.FuncUtils.pred;
import static sjtu.ipads.wtune.common.utils.IterableSupport.all;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast1.constants.LiteralKind.TEXT;

class Clean {
  static void clean(SqlNode root) {
    SqlNode target;
    while ((target = NodeCollector.locate(root, Clean::isBoolConstant)) != null)
      deleteBoolConstant(target);

    for (SqlNode node : NodeCollector.collect(root, Clean::isTextFunc)) inlineTextConstant(node);
  }

  private static boolean isConstant(SqlNode node) {
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
        return all(node.$(Tuple_Exprs), Clean::isConstant);
      case FuncCall:
        return node.$(FuncCall_Name) != null
            && !node.$(FuncCall_Name).$(Name2_1).contains("rand")
            && all(node.$(FuncCall_Args), Clean::isConstant);
      case Match:
        return isConstant(node.$(Match_Expr)) && all(node.$(Match_Cols), Clean::isConstant);
      case Case:
        final SqlNode cond = node.$(Case_Cond);
        final SqlNode else_ = node.$(Case_Else);
        return (cond == null || isConstant(cond))
            && (else_ == null || isConstant(else_))
            && all(node.$(Case_Whens), Clean::isConstant);
      case When:
        return isConstant(node.$(When_Cond)) && isConstant(node.$(When_Expr));
    }
    return false;
  }

  private static boolean isBoolConstant(SqlNode node) {
    final SqlNode parent = node.parent();
    return parent != null
        && (nodeEquals(node, parent.$(QuerySpec_Where))
            || Binary.isInstance(parent) && parent.$(Binary_Op).isLogic())
        && isConstant(node);
  }

  private static void deleteBoolConstant(SqlNode node) {
    final SqlNode parent = node.parent();

    if (nodeEquals(node, parent.$(QuerySpec_Where))) {
      parent.remove(QuerySpec_Where);

    } else if (Binary.isInstance(parent) && parent.$(Binary_Op).isLogic()) {
      final SqlNode lhs = parent.$(Binary_Left), rhs = parent.$(Binary_Right);
      if (nodeEquals(lhs, node)) node.context().displaceNode(parent.nodeId(), rhs.nodeId());
      else if (nodeEquals(rhs, node)) node.context().displaceNode(parent.nodeId(), lhs.nodeId());
      else assert false;
    }
  }

  private static boolean isTextFunc(SqlNode node) {
    final SqlNode name = node.$(FuncCall_Name);
    if (name == null || name.$(Name2_0) != null) return false; // UDF

    final String funcName = name.$(Name2_1).toLowerCase();
    final List<SqlNode> args = node.$(FuncCall_Args);

    switch (funcName) {
      case "concat":
        return all(args, pred(Literal::isInstance).or(Clean::isTextFunc));
      case "lower":
      case "upper":
        return args.size() == 1 && (Literal.isInstance(args.get(0)) || isTextFunc(args.get(0)));
      default:
        return false;
    }
  }

  private static void inlineTextConstant(SqlNode funcCall) {
    final SqlNode literal = SqlSupport.mkLiteral(funcCall.context(), TEXT, stringify(funcCall));
    funcCall.context().displaceNode(funcCall.nodeId(), literal.nodeId());
  }

  private static String stringify(SqlNode node) {
    if (Literal.isInstance(node)) return String.valueOf(node.$(Literal_Value));
    assert FuncCall.isInstance(node);

    final String funcName = node.$(FuncCall_Name).$(Name2_1).toLowerCase();
    final SqlNodes args = node.$(FuncCall_Args);

    switch (funcName) {
      case "concat":
        return joining("", args, Clean::stringify);
      case "upper":
        return stringify(args.get(0)).toUpperCase();
      case "lower":
        return stringify(args.get(0)).toLowerCase();
      default:
        assert false;
        return "";
    }
  }
}
