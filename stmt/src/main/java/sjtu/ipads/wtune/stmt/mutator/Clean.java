package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.stmt.utils.Collector;

import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.FuncUtils.pred;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;

class Clean {
  public static ASTNode clean(ASTNode node) {
    Collector.collect(node, Clean::isConstant).forEach(Clean::deleteBoolLiteral);
    Collector.collect(node, Clean::isTextFunc).forEach(Clean::stringify);
    return node;
  }

  private static boolean isConstant(ASTNode node) {
    final ExprType exprKind = node.get(EXPR_KIND);
    if (exprKind == LITERAL || exprKind == ExprType.SYMBOL) return true;

    if (exprKind == ExprType.CAST) return isConstant(node.get(CAST_EXPR));
    if (exprKind == ExprType.COLLATE) return isConstant(node.get(COLLATE_EXPR));
    if (exprKind == ExprType.INTERVAL) return isConstant(node.get(INTERVAL_EXPR));
    if (exprKind == ExprType.CONVERT_USING) return isConstant(node.get(CONVERT_USING_EXPR));
    if (exprKind == ExprType.DEFAULT) return isConstant(node.get(DEFAULT_COL));
    if (exprKind == ExprType.VALUES) return isConstant(node.get(VALUES_EXPR));

    if (exprKind == ExprType.UNARY) return isConstant(node.get(UNARY_EXPR));
    if (exprKind == BINARY)
      return isConstant(node.get(BINARY_LEFT)) && isConstant(node.get(BINARY_RIGHT));
    if (exprKind == ExprType.TERNARY)
      return isConstant(node.get(TERNARY_LEFT))
          && isConstant(node.get(TERNARY_MIDDLE))
          && isConstant(node.get(TERNARY_RIGHT));

    if (exprKind == ExprType.TUPLE)
      return node.get(TUPLE_EXPRS).stream().allMatch(Clean::isConstant);
    if (exprKind == ExprType.FUNC_CALL)
      return !node.get(FUNC_CALL_NAME).get(NAME_2_1).contains("rand")
          && node.get(FUNC_CALL_ARGS).stream().allMatch(Clean::isConstant);

    if (exprKind == ExprType.MATCH)
      return isConstant(node.get(MATCH_EXPR))
          && node.get(MATCH_COLS).stream().allMatch(Clean::isConstant);

    if (exprKind == ExprType.CASE) {
      final ASTNode cond = node.get(CASE_COND);
      final ASTNode _else = node.get(CASE_ELSE);
      return (cond == null || isConstant(cond))
          && node.get(CASE_WHENS).stream().allMatch(Clean::isConstant)
          && (_else == null || isConstant(_else));
    }

    if (exprKind == ExprType.WHEN)
      return isConstant(node.get(WHEN_COND)) && isConstant(node.get(WHEN_EXPR));

    return false;
  }

  private static void deleteBoolLiteral(ASTNode node) {
    final ASTNode parent = node.parent();

    if (parent.get(QUERY_SPEC_WHERE) == node) {
      parent.unset(QUERY_SPEC_WHERE);

    } else if (BINARY.isInstance(parent) && parent.get(BINARY_OP).isLogic()) {
      final ASTNode left = parent.get(BINARY_LEFT);
      final ASTNode right = parent.get(BINARY_RIGHT);

      if (left == node) parent.update(right);
      else if (right == node) parent.update(left);
      else assert false;
    }
  }

  private static boolean isTextFunc(ASTNode node) {
    final ASTNode name = node.get(FUNC_CALL_NAME);
    if (name == null || name.get(NAME_2_0) != null) return false; // UDF

    final String funcName = name.get(NAME_2_1).toLowerCase();
    final List<ASTNode> args = node.get(FUNC_CALL_ARGS);

    switch (funcName) {
      case "concat":
        return args.stream().allMatch(pred(LITERAL::isInstance).or(Clean::isTextFunc));
      case "lower":
      case "upper":
        return args.size() == 1 && (LITERAL.isInstance(args.get(0)) || isTextFunc(args.get(0)));
      default:
        return false;
    }
  }

  private static void stringify(ASTNode funcCall) {
    final ASTNode literal = ASTNode.expr(LITERAL);
    literal.set(LITERAL_TYPE, LiteralType.TEXT);
    literal.set(LITERAL_VALUE, stringify0(funcCall));

    funcCall.update(literal);
  }

  private static String stringify0(ASTNode node) {
    if (LITERAL.isInstance(node)) return String.valueOf(node.get(LITERAL_VALUE));
    assert FUNC_CALL.isInstance(node);

    final String funcName = node.get(FUNC_CALL_NAME).get(NAME_2_1).toLowerCase();
    final List<ASTNode> args = node.get(FUNC_CALL_ARGS);

    switch (funcName) {
      case "concat":
        return args.stream().map(Clean::stringify0).collect(Collectors.joining());
      case "upper":
        return stringify0(args.get(0)).toUpperCase();
      case "lower":
        return stringify0(args.get(0)).toLowerCase();
      default:
        throw new IllegalArgumentException();
    }
  }
}
