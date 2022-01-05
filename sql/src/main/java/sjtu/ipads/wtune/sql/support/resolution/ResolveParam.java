package sjtu.ipads.wtune.sql.support.resolution;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ast1.ExprKind;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.constants.BinaryOpKind;
import sjtu.ipads.wtune.sql.ast1.constants.Category;
import sjtu.ipads.wtune.sql.ast1.constants.UnaryOpKind;
import sjtu.ipads.wtune.sql.schema.Column;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.sql.SqlSupport.getAnotherSide;
import static sjtu.ipads.wtune.sql.SqlSupport.isPrimitivePredicate;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.Expr_Kind;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.Name2_1;
import static sjtu.ipads.wtune.sql.ast1.constants.UnaryOpKind.NOT;
import static sjtu.ipads.wtune.sql.ast1.constants.UnaryOpKind.UNARY_MINUS;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.Type.*;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.modifier;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;

class ResolveParam {
  private boolean negated;
  private LinkedList<ParamModifier> stack;

  private ResolveParam() {}

  static List<ParamDesc> resolve(SqlNode expr) {
    return new ResolveParam().resolve0(expr);
  }

  private List<ParamDesc> resolve0(SqlNode expr) {
    if (!isPrimitivePredicate(expr))
      throw new IllegalArgumentException("only accept primitive predicate");

    final List<SqlNode> params = collectParams(expr);
    if (params.isEmpty()) return emptyList();

    return ListSupport.map(params, it -> resolve0(expr, it));
  }

  private ParamDesc resolve0(SqlNode expr, SqlNode paramNode) {
    // determine if the expr is negated
    boolean negated = false;
    SqlNode parent = expr.parent();
    while (Expr.isInstance(parent)) { // trace back to expr root
      if (parent.get(Unary_Op) == NOT) negated = !negated;
      parent = parent.parent();
    }

    this.negated = negated;
    this.stack = new LinkedList<>();

    SqlNode cursor = paramNode;
    do {
      if (!deduce(cursor)) return null;
      cursor = cursor.parent();
    } while (cursor != expr);

    if (stack.getFirst().type() == GUESS) {
      stack.removeFirst();
      if (Literal.isInstance(paramNode)) {
        stack.offerFirst(modifier(DIRECT_VALUE, paramNode.$(Literal_Value)));
      } else {
        stack.offerFirst(modifier(DIRECT_VALUE, "UNKNOWN"));
      }
    }

    return new ParamDescImpl(expr, paramNode, stack);
  }

  private boolean deduce(SqlNode target) {
    final SqlNode parent = target.parent();
    final ExprKind exprKind = parent.$(Expr_Kind);
    final boolean negated = this.negated;

    switch (exprKind) {
      case Unary:
        {
          final UnaryOpKind op = parent.$(Unary_Op);
          if (op == UNARY_MINUS) stack.offerFirst(modifier(INVERSE));
          return op != UnaryOpKind.BINARY;
        }

      case Binary:
        {
          final SqlNode otherSide = getAnotherSide(parent, target);
          assert otherSide != null;
          // `swapped` indicates whether the param is of the right side.
          // e.g. For "age > ?" ? should be LESS than age.
          //      For "? > age" ? should be GREATER than age.
          final boolean swapped = nodeEquals(parent.$(Binary_Right), target);
          final BinaryOpKind op = parent.$(Binary_Op);

          final ParamModifier modifier = ParamModifier.fromBinaryOp(op, target, swapped, negated);
          if (modifier == null) return false;
          if (modifier.type() != KEEP) stack.offerFirst(modifier);

          return induce(otherSide);
        }

      case Tuple:
        stack.offerFirst(modifier(TUPLE_ELEMENT));
        return true;

      case Array:
        stack.offerFirst(modifier(ARRAY_ELEMENT));
        return true;

      case Ternary:
        if (nodeEquals(parent.$(Ternary_Left), target))
          stack.offerFirst(modifier(negated ? INCREASE : DECREASE));
        else if (parent.get(Ternary_Middle) == target)
          stack.offerFirst(modifier(negated ? DECREASE : INCREASE));
        else return false;
        return induce(parent.$(Ternary_Left));

      case Match:
        {
          final List<SqlNode> cols = parent.$(Match_Cols);
          if (cols.size() > 1) return false;
          stack.offerFirst(modifier(MATCHING));
          return induce(parent.$(Match_Cols).get(0));
        }

      case FuncCall:
        {
          final String funcName = parent.$(FuncCall_Name).$(Name2_1).toLowerCase();
          if ("to_days".equals(funcName)) return true;
        }

      default:
        return false;
    }
  }

  // Induce an expression's value and express as modifier.
  // e.g. `x` + 1 (where `x` is a column name),
  // the resultant modifiers is [Value("x"), DirectValue(1), Plus()]
  private boolean induce(SqlNode target) {
    final ExprKind exprKind = target.$(Expr_Kind);

    switch (exprKind) {
      case ColRef:
        {
          final Attribute reference = ResolutionSupport.traceRef(ResolutionSupport.resolveAttribute(target));
          final Column column = reference == null ? null : reference.column();
          if (reference == null || column == null) {
            stack.offerFirst(modifier(GUESS));

          } else {
            final Relation relation = reference.owner();
            stack.offerFirst(modifier(COLUMN_VALUE, relation, column));
          }
          return true;
        }

      case FuncCall:
        {
          final List<SqlNode> args = target.$(FuncCall_Args);
          final String funcName = target.$(FuncCall_Name).toString().toLowerCase();
          stack.offerFirst(modifier(INVOKE_FUNC, funcName, args.size()));
          for (SqlNode arg : Lists.reverse(args)) if (!induce(arg)) return false;
          return true;
        }

      case Binary:
        {
          switch (target.$(Binary_Op)) {
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
          return induce(target.$(Binary_Right)) && induce(target.$(Binary_Left));
        }

      case Aggregate:
        stack.offerFirst(modifier(INVOKE_AGG, target.get(Aggregate_Name)));
        return true;

      case Cast:
        return target.$(Cast_Type).category() != Category.INTERVAL && induce(target.$(Cast_Expr));

      case Literal:
        stack.offerFirst(modifier(DIRECT_VALUE, target.get(Literal_Value)));
        return true;

      case Tuple:
        {
          final List<SqlNode> exprs = target.$(Tuple_Exprs);
          stack.offerFirst(modifier(MAKE_TUPLE, exprs.size()));
          for (SqlNode elements : Lists.reverse(exprs)) if (!induce(elements)) return false;
          return true;
        }

      case Symbol:
        stack.offerFirst(modifier(DIRECT_VALUE, target.get(Symbol_Text)));
        return true;

      case QueryExpr:
        stack.offerFirst(modifier(GUESS));
        return true;

      default:
        return false;
    }
  }

  private static List<SqlNode> collectParams(SqlNode expr) {
    return nodeLocator().accept(ResolveParam::isParam).stopIfNot(Expr).gather(expr);
  }

  private static boolean isParam(SqlNode expr) {
    return (Literal.isInstance(expr) && !FuncCall.isInstance(expr.parent()))
        || Param.isInstance(expr)
        || FuncCall.isInstance(expr) && "now".equalsIgnoreCase(expr.$(FuncCall_Name).$(Name2_1));
  }
}
