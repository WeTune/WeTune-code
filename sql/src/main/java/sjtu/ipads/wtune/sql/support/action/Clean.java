package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;

import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.FuncSupport.pred;
import static sjtu.ipads.wtune.common.utils.IterableSupport.all;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast.constants.LiteralKind.TEXT;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;

class Clean {
  static void clean(SqlNode root) {
    SqlNode target;
    while ((target = nodeLocator().accept(Clean::isBoolConstant).find(root)) != null)
      deleteBoolConstant(target);

    for (SqlNode node : nodeLocator().accept(Clean::isTextFunc).gather(root))
      inlineTextConstant(node);
  }

  private static boolean isBoolConstant(SqlNode node) {
    final SqlNode parent = node.parent();
    return parent != null
        && (nodeEquals(node, parent.$(QuerySpec_Where))
            || Binary.isInstance(parent) && parent.$(Binary_Op).isLogic())
        && NormalizationSupport.isConstant(node);
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
