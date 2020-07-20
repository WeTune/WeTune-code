package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.Arrays;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.stmt.attrs.ParamModifier.Type.*;

public class ParamModifier {
  public enum Type {
    INVERSE,
    SUBTRACT,
    ADD,
    DIVIDE,
    TIMES,
    TUPLE_ELEMENT,
    ARRAY_ELEMENT,
    DECREASE,
    INCREASE,
    LIKE,
    REGEX,
    CHECK_NULL,
    CHECK_NULL_NOT,
    CHECK_BOOL,
    CHECK_BOOL_NOT,
    NEQ,
    COLUMN_VALUE,
    INVOKE_FUNC,
    INVOKE_AGG,
    DIRECT_VALUE,
    MAKE_TUPLE,
    MATCHING,
    GEN_OFFSET,
    GUESS,
    KEEP
  }

  private final Type type;
  private final Object[] args;

  private ParamModifier(Type type, Object[] args) {
    this.type = type;
    this.args = args;
  }

  public Type type() {
    return type;
  }

  public Object[] args() {
    return args;
  }

  public static ParamModifier of(Type type, Object... args) {
    return new ParamModifier(type, args);
  }

  private static ParamModifier fromLike(SQLNode param) {
    if (exprKind(param) == SQLExpr.Kind.LITERAL) {
      final String value = param.get(LITERAL_VALUE).toString();
      return ParamModifier.of(LIKE, value.startsWith("%"), value.endsWith("%"));
    }
    return ParamModifier.of(LIKE);
  }

  private static ParamModifier fromIs(SQLNode param, boolean not) {
    if (exprKind(param) == SQLExpr.Kind.LITERAL) {
      if (param.get(LITERAL_TYPE) == LiteralType.NULL)
        return not ? of(CHECK_NULL_NOT) : of(CHECK_NULL);
      else if (param.get(LITERAL_TYPE) == LiteralType.BOOL)
        return not ? of(CHECK_BOOL_NOT) : of(CHECK_BOOL);
      else return assertFalse();

    } else return assertFalse();
  }

  private static final ParamModifier KEEP_STILL = of(KEEP);

  public static ParamModifier fromBinaryOp(
      SQLExpr.BinaryOp op, SQLNode target, boolean inverse, boolean not) {
    if (op == SQLExpr.BinaryOp.EQUAL
        || op == SQLExpr.BinaryOp.IN_LIST
        || op == SQLExpr.BinaryOp.ARRAY_CONTAINS) return not ? of(NEQ) : KEEP_STILL;

    if (op == SQLExpr.BinaryOp.NOT_EQUAL) return not ? KEEP_STILL : of(NEQ);

    if (op == SQLExpr.BinaryOp.GREATER_OR_EQUAL || op == SQLExpr.BinaryOp.GREATER_THAN)
      return of(inverse ^ not ? DECREASE : INCREASE);

    if (op == SQLExpr.BinaryOp.LESS_OR_EQUAL || op == SQLExpr.BinaryOp.LESS_THAN)
      return of(inverse ^ not ? INCREASE : DECREASE);

    if (op == SQLExpr.BinaryOp.LIKE
        || op == SQLExpr.BinaryOp.ILIKE
        || op == SQLExpr.BinaryOp.SIMILAR_TO) return not ? of(NEQ) : fromLike(target);

    if (op == SQLExpr.BinaryOp.IS) return fromIs(target, not);

    if (op.standard() == SQLExpr.BinaryOp.REGEXP) return not ? of(NEQ) : of(REGEX);

    if (op == SQLExpr.BinaryOp.PLUS) return of(SUBTRACT);
    if (op == SQLExpr.BinaryOp.MINUS) return of(ADD);
    if (op == SQLExpr.BinaryOp.MULT) return of(DIVIDE);
    if (op == SQLExpr.BinaryOp.DIV) return of(TIMES);

    // omit others since not encountered
    return null;
  }

  @Override
  public String toString() {
    return type + Arrays.toString(args);
  }
}
