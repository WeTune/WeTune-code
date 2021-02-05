package sjtu.ipads.wtune.stmt.resolver;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp.NOT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp.UNARY_MINUS;
import static sjtu.ipads.wtune.sqlparser.rel.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.stmt.resolver.BoolExprManager.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.resolver.ParamManager.PARAM;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.*;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.fromBinaryOp;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.modifier;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveBoolExpr;

/**
 * Mark parameter in a statement.
 *
 * <p>SQLNode of following types may be marked as parameters:
 *
 * <ul>
 *   <li>Literal
 *   <li>ParamMarker
 * </ul>
 *
 * The class fine-grained calculates the parameter value. The basic idea is deducing the value of a
 * parameter from the related column value.
 *
 * <p>For example, say we have a predicate
 *
 * <pre>"(`x` + 1) * `y` < ? + 3"</pre>
 *
 * , where `x`, `y` is a column name, and ? is the parameter. Given a row in database, the target is
 * to determine the value of ? that make the predicate evaluate to TRUE against the row. i.e.
 * calculate ? by the following expression:
 *
 * <pre>? > ((row.x + 1) * row.y) - 3</pre>
 *
 * Such process is implemented by a stack-based evaluation process:
 *
 * <ol>
 *   <li>Init a stack
 *   <li>Retrieve the value of column `x` in this row, and push it to the stack
 *   <li>Push constant value 1 to the stack
 *   <li>Pop 2 values from the stack, add them and push back to the stack
 *   <li>Retrieve the value of column `y` in this row, and push it to the stack
 *   <li>Pop 2 values from the stack, times them and push back to the stack
 *   <li>Pop 1 value from the stack, increase the value and push back to the stack
 *   <li>Push constant value 3 to the stack
 *   <li>Pop 2 value to the stack, and minus the second one by the first one, and push the result
 *   <li>The value on the top of stack is taken to be the parameter's value
 * </ol>
 */
class ResolveParamFull implements ASTVistor {
  private int nextIndex;

  @Override
  public boolean enterParamMarker(ASTNode paramMarker) {
    resolveParam(paramMarker);
    return false;
  }

  @Override
  public boolean enterLiteral(ASTNode literal) {
    resolveParam(literal);
    return false;
  }

  @Override
  public boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
    if (key == QUERY_LIMIT) {
      if (child != null && PARAM_MARKER.isInstance(child)) {
        child.unset(PARAM_MARKER_NUMBER);
        child.set(EXPR_KIND, LITERAL);
        child.set(LITERAL_TYPE, LiteralType.INTEGER);
        child.set(LITERAL_VALUE, 10);
      }
      return false;

    } else if (key == QUERY_OFFSET) {
      if (child != null)
        child.set(PARAM, new Param(child, nextIndex++, singletonList(modifier(GEN_OFFSET))));
      return false;
    }

    return true;
  }

  private void resolveParam(ASTNode startPoint) {
    // identify the scope concerning this param
    final Pair<ASTNode, ASTNode> pair = boolScope(startPoint);
    if (pair == null) return;

    final ASTNode ctx = pair.getLeft();

    // traceback to the expression root
    // to determine whether the predicate is negated
    boolean negated = false;
    ASTNode parent = ctx.parent();
    while (EXPR.isInstance(parent)) {
      if (parent.get(UNARY_OP) == NOT) negated = !negated;
      parent = parent.parent();
    }

    // deduce the param from the value in a DB row
    final LinkedList<ParamModifier> modifierStack = new LinkedList<>();
    ASTNode p = startPoint;

    do {
      if (!deduce(p, modifierStack, negated)) return;
      p = p.parent();
    } while (p != ctx);

    if (modifierStack.getLast().type() == GUESS) {
      modifierStack.removeLast();
      if (LITERAL.isInstance(startPoint))
        modifierStack.add(modifier(DIRECT_VALUE, startPoint.get(LITERAL_VALUE)));
      else modifierStack.add(modifier(DIRECT_VALUE, "UNKNOWN"));
    }

    startPoint.set(PARAM, new Param(startPoint, nextIndex++, modifierStack));
  }

  // find the nearest ancestor of `startPoint` that
  //   1. itself is a bool expression
  //   2. none of its children is a bool expression
  private static Pair<ASTNode, ASTNode> boolScope(ASTNode startPoint) {
    ASTNode child = startPoint, parent = startPoint.parent();

    while (EXPR.isInstance(parent) && parent.get(BOOL_EXPR) == null) {
      child = parent;
      parent = parent.parent();
    }

    return parent.get(BOOL_EXPR) == null ? null : Pair.of(parent, child);
  }

  // deduce the restriction on a parameter's value and express as modifier
  // e.g. `x` > ? (where `x` is a column name, ? is the parameter), then ? should satisfies "< a"
  // the resultant modifiers is [Value("x"), Decrease()]
  private static boolean deduce(ASTNode target, List<ParamModifier> stack, boolean negated) {
    final ASTNode parent = target.parent();
    final ExprType exprKind = parent.get(EXPR_KIND);

    if (exprKind == UNARY) {
      final UnaryOp op = parent.get(UNARY_OP);
      if (op == UNARY_MINUS) stack.add(modifier(INVERSE));
      // omit others since not encountered
      return op != UnaryOp.BINARY;

    } else if (exprKind == BINARY) {
      // if target is on the right, the operator should be reversed
      final ASTNode left = parent.get(BINARY_LEFT);
      final ASTNode right = parent.get(BINARY_RIGHT);
      final boolean inverseOp = right == target;
      final ASTNode otherSide = inverseOp ? left : right;
      final BinaryOp op = parent.get(BINARY_OP);

      final ParamModifier modifier = fromBinaryOp(op, target, inverseOp, negated);
      if (modifier == null) return false;

      if (modifier.type() != KEEP) stack.add(modifier);

      return induce(otherSide, stack);

    } else if (exprKind == TUPLE) {
      stack.add(modifier(TUPLE_ELEMENT));
      return true;

    } else if (exprKind == ARRAY) {
      stack.add(modifier(ARRAY_ELEMENT));
      return true;

    } else if (exprKind == TERNARY) {
      if (parent.get(TERNARY_MIDDLE) == target) stack.add(modifier(DECREASE));
      else if (parent.get(TERNARY_RIGHT) == target) stack.add(modifier(INCREASE));
      else return assertFalse();

      return induce(parent.get(TERNARY_LEFT), stack);

    } else if (exprKind == MATCH) {
      stack.add(modifier(MATCHING));
      return induce(parent.get(MATCH_COLS).get(0), stack);

    } else return false;
  }

  // induce a expression's value and express as modifier
  // e.g. `x` + 1 (where `x` is a column name),
  // the resultant modifiers is [Value("x"), DirectValue(1), Plus()]
  private static boolean induce(ASTNode target, List<ParamModifier> stack) {
    final ExprType exprKind = target.get(EXPR_KIND);

    if (exprKind == COLUMN_REF) {
      final Column column = target.get(ATTRIBUTE).column(true);
      if (column == null) stack.add(modifier(GUESS));
      else stack.add(modifier(COLUMN_VALUE, column.tableName(), column.name(), /* position */ 0));

    } else if (exprKind == FUNC_CALL) {
      final List<ASTNode> args = target.get(FUNC_CALL_ARGS);
      stack.add(
          modifier(INVOKE_FUNC, target.get(FUNC_CALL_NAME).toString().toLowerCase(), args.size()));
      for (ASTNode arg : Lists.reverse(args)) if (!induce(arg, stack)) return false;

    } else if (exprKind == BINARY) {
      switch (target.get(BINARY_OP)) {
        case PLUS:
          stack.add(modifier(ADD));
          break;
        case MINUS:
          stack.add(modifier(SUBTRACT));
          break;
        case MULT:
          stack.add(modifier(TIMES));
          break;
        case DIV:
          stack.add(modifier(DIVIDE));
          break;
        default:
          return false;
      }

      return induce(target.get(BINARY_RIGHT), stack) && induce(target.get(BINARY_LEFT), stack);

    } else if (exprKind == AGGREGATE) {
      stack.add(modifier(INVOKE_AGG, target.get(AGGREGATE_NAME)));

    } else if (exprKind == CAST) {
      if (target.get(CAST_TYPE).category() == Category.INTERVAL) return false;

      induce(target.get(CAST_EXPR), stack);

    } else if (exprKind == LITERAL) {
      stack.add(modifier(DIRECT_VALUE, target.get(LITERAL_VALUE)));

    } else if (exprKind == TUPLE) {
      final List<ASTNode> exprs = target.get(TUPLE_EXPRS);
      stack.add(modifier(MAKE_TUPLE, exprs.size()));
      for (ASTNode elements : Lists.reverse(exprs)) if (!induce(elements, stack)) return false;

    } else if (exprKind == SYMBOL) {
      stack.add(modifier(DIRECT_VALUE, target.get(SYMBOL_TEXT)));

    } else if (exprKind == QUERY_EXPR) {
      stack.add(modifier(GUESS));
    } else return false;

    return true;
  }

  public static ParamManager resolve(ASTNode node) {
    if (node.manager(BoolExprManager.class) == null) resolveBoolExpr(node);

    if (node.manager(ParamManager.class) == null)
      node.context().addManager(ParamManager.class, ParamManager.build());

    node.accept(new ResolveParamFull());
    return node.manager(ParamManager.class);
  }
}
