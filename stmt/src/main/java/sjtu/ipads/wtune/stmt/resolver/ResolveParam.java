package sjtu.ipads.wtune.stmt.resolver;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CAST_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CAST_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_ARGS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.MATCH_COLS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.SYMBOL_TEXT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TERNARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TERNARY_MIDDLE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TERNARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TUPLE_EXPRS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.UNARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_2_1;
import static sjtu.ipads.wtune.sqlparser.ast.constants.Category.INTERVAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.FUNC_CALL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.PARAM_MARKER;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp.NOT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp.UNARY_MINUS;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.locateOtherSide;
import static sjtu.ipads.wtune.stmt.resolver.BoolExprManager.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.ADD;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.ARRAY_ELEMENT;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.COLUMN_VALUE;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.DECREASE;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.DIRECT_VALUE;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.DIVIDE;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.GUESS;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.INCREASE;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.INVERSE;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.INVOKE_AGG;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.INVOKE_FUNC;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.KEEP;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.MAKE_TUPLE;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.MATCHING;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.SUBTRACT;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.TIMES;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.TUPLE_ELEMENT;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.fromBinaryOp;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.modifier;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;

class ResolveParam {
  private boolean negated;
  private Deque<ParamModifier> stack;

  private ResolveParam() {}

  public static List<ParamDesc> resolve(ASTNode expr) {
    return new ResolveParam().resolve0(expr);
  }

  private List<ParamDesc> resolve0(ASTNode expr) {
    final BoolExpr boolExpr = expr.get(BOOL_EXPR);
    if (boolExpr == null || !boolExpr.isPrimitive())
      throw new IllegalArgumentException("only accept primitive predicate");

    final List<ASTNode> params = collectParams(expr);
    if (params.isEmpty()) return null;

    return listMap(it -> resolve0(expr, it), params);
  }

  private ParamDesc resolve0(ASTNode expr, ASTNode paramNode) {
    // determine if the expr is negated
    boolean negated = false;
    ASTNode parent = expr.parent();
    while (EXPR.isInstance(parent)) { // trace back to expr root
      if (parent.get(UNARY_OP) == NOT) negated = !negated;
      parent = parent.parent();
    }

    this.negated = negated;
    this.stack = new LinkedList<>();

    ASTNode cursor = paramNode;
    do {
      if (!deduce(cursor)) return null;
      cursor = cursor.parent();
    } while (cursor != expr);

    if (stack.getFirst().type() == GUESS) {
      stack.removeFirst();
      if (LITERAL.isInstance(paramNode)) {
        stack.offerFirst(modifier(DIRECT_VALUE, paramNode.get(LITERAL_VALUE)));
      } else {
        stack.offerFirst(modifier(DIRECT_VALUE, "UNKNOWN"));
      }
    }

    return new ParamDescImpl(expr, paramNode, stack);
  }

  private boolean deduce(ASTNode target) {
    final ASTNode parent = target.parent();
    final ExprKind exprKind = parent.get(EXPR_KIND);
    final boolean negated = this.negated;

    switch (exprKind) {
      case UNARY:
        {
          final UnaryOp op = parent.get(UNARY_OP);
          if (op == UNARY_MINUS) stack.offerFirst(modifier(INVERSE));
          return op != UnaryOp.BINARY;
        }

      case BINARY:
        {
          final ASTNode otherSide = locateOtherSide(parent, target);
          assert otherSide != null;
          // `swapped` indicates whether the param is of the right side.
          // e.g. age > ?, ? should be LESS than age
          //      ? > age, ? should be GREATER than age
          final boolean swapped = parent.get(BINARY_RIGHT) == target;
          final BinaryOp op = parent.get(BINARY_OP);

          final ParamModifier modifier = fromBinaryOp(op, target, swapped, negated);
          if (modifier == null) return false;
          if (modifier.type() != KEEP) stack.offerFirst(modifier);

          return induce(otherSide);
        }

      case TUPLE:
        stack.offerFirst(modifier(TUPLE_ELEMENT));
        return true;

      case ARRAY:
        stack.offerFirst(modifier(ARRAY_ELEMENT));
        return true;

      case TERNARY:
        if (parent.get(TERNARY_MIDDLE) == target)
          stack.offerFirst(modifier(negated ? INCREASE : DECREASE));
        else if (parent.get(TERNARY_RIGHT) == target)
          stack.offerFirst(modifier(negated ? DECREASE : INCREASE));
        else return false;
        return induce(parent.get(TERNARY_LEFT));

      case MATCH:
        {
          final List<ASTNode> cols = parent.get(MATCH_COLS);
          if (cols.size() > 1) return false;
          stack.offerFirst(modifier(MATCHING));
          return induce(parent.get(MATCH_COLS).get(0));
        }

      default:
        return false;
    }
  }

  // induce a expression's value and express as modifier
  // e.g. `x` + 1 (where `x` is a column name),
  // the resultant modifiers is [Value("x"), DirectValue(1), Plus()]
  private boolean induce(ASTNode target) {
    final ExprKind exprKind = target.get(EXPR_KIND);

    switch (exprKind) {
      case COLUMN_REF:
        {
          final Attribute reference = target.get(ATTRIBUTE).reference(true);
          final Column column = reference == null ? null : reference.column(true);
          if (reference == null || column == null) {
            stack.offerFirst(modifier(GUESS));

          } else {
            final Relation relation = reference.owner();
            stack.offerFirst(modifier(COLUMN_VALUE, relation, column));
          }
          return true;
        }

      case FUNC_CALL:
        {
          final List<ASTNode> args = target.get(FUNC_CALL_ARGS);
          final String funcName = target.get(FUNC_CALL_NAME).toString().toLowerCase();
          stack.offerFirst(modifier(INVOKE_FUNC, funcName, args.size()));
          for (ASTNode arg : Lists.reverse(args)) if (!induce(arg)) return false;
          return true;
        }

      case BINARY:
        {
          switch (target.get(BINARY_OP)) {
            case PLUS:
              stack.offerFirst(modifier(ADD));
              break;
            case MINUS:
              stack.offerFirst(modifier(SUBTRACT));
              break;
            case MULT:
              stack.offerFirst(modifier(TIMES));
              break;
            case DIV:
              stack.offerFirst(modifier(DIVIDE));
              break;
            default:
              return false;
          }
          return induce(target.get(BINARY_RIGHT)) && induce(target.get(BINARY_LEFT));
        }

      case AGGREGATE:
        stack.offerFirst(modifier(INVOKE_AGG, target.get(AGGREGATE_NAME)));
        return true;

      case CAST:
        return target.get(CAST_TYPE).category() != INTERVAL && induce(target.get(CAST_EXPR));

      case LITERAL:
        stack.offerFirst(modifier(DIRECT_VALUE, target.get(LITERAL_VALUE)));
        return true;

      case TUPLE:
        {
          final List<ASTNode> exprs = target.get(TUPLE_EXPRS);
          stack.offerFirst(modifier(MAKE_TUPLE, exprs.size()));
          for (ASTNode elements : Lists.reverse(exprs)) if (!induce(elements)) return false;
          return true;
        }

      case SYMBOL:
        stack.offerFirst(modifier(DIRECT_VALUE, target.get(SYMBOL_TEXT)));
        return true;

      case QUERY_EXPR:
        stack.offerFirst(modifier(GUESS));
        return true;

      default:
        return false;
    }
  }

  private static boolean isParam(ASTNode expr) {
    return LITERAL.isInstance(expr) && !FUNC_CALL.isInstance(expr.parent())
        || PARAM_MARKER.isInstance(expr)
        || FUNC_CALL.isInstance(expr)
            && "now".equalsIgnoreCase(expr.get(FUNC_CALL_NAME).get(NAME_2_1));
  }

  private List<ASTNode> collectParams(ASTNode expr) {
    final CollectParams collector = new CollectParams();
    expr.accept(collector);
    return collector.params;
  }

  private static class CollectParams implements ASTVistor {
    private final List<ASTNode> params = new ArrayList<>(2);

    @Override
    public boolean enter(ASTNode node) {
      if (isParam(node)) params.add(node);
      return true;
    }

    @Override
    public boolean enterQuery(ASTNode query) {
      return false;
    }
  }
}
