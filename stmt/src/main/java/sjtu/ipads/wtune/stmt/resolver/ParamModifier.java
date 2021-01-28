package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;

import java.util.Arrays;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.LITERAL;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.*;

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

  public static ParamModifier modifier(Type type, Object... args) {
    return new ParamModifier(type, args);
  }

  private static ParamModifier fromLike(SQLNode param) {
    if (LITERAL.isInstance(param)) {
      final String value = param.get(LITERAL_VALUE).toString();
      return ParamModifier.modifier(LIKE, value.startsWith("%"), value.endsWith("%"));
    }
    return ParamModifier.modifier(LIKE);
  }

  private static ParamModifier fromIs(SQLNode param, boolean not) {
    if (LITERAL.isInstance(param)) {
      if (param.get(LITERAL_TYPE) == LiteralType.NULL)
        return not ? modifier(CHECK_NULL_NOT) : modifier(CHECK_NULL);
      else if (param.get(LITERAL_TYPE) == LiteralType.BOOL)
        return not ? modifier(CHECK_BOOL_NOT) : modifier(CHECK_BOOL);
      else return assertFalse();

    } else return assertFalse();
  }

  private static final ParamModifier KEEP_STILL = modifier(KEEP);

  public static ParamModifier fromBinaryOp(
      BinaryOp op, SQLNode target, boolean inverse, boolean not) {
    if (op == BinaryOp.EQUAL || op == BinaryOp.IN_LIST || op == BinaryOp.ARRAY_CONTAINS)
      return not ? modifier(NEQ) : KEEP_STILL;

    if (op == BinaryOp.NOT_EQUAL) return not ? KEEP_STILL : modifier(NEQ);

    if (op == BinaryOp.GREATER_OR_EQUAL || op == BinaryOp.GREATER_THAN)
      return modifier(inverse ^ not ? DECREASE : INCREASE);

    if (op == BinaryOp.LESS_OR_EQUAL || op == BinaryOp.LESS_THAN)
      return modifier(inverse ^ not ? INCREASE : DECREASE);

    if (op == BinaryOp.LIKE || op == BinaryOp.ILIKE || op == BinaryOp.SIMILAR_TO)
      return not ? modifier(NEQ) : fromLike(target);

    if (op == BinaryOp.IS) return fromIs(target, not);

    if (op.standard() == BinaryOp.REGEXP) return not ? modifier(NEQ) : modifier(REGEX);

    if (op == BinaryOp.PLUS) return modifier(SUBTRACT);
    if (op == BinaryOp.MINUS) return modifier(ADD);
    if (op == BinaryOp.MULT) return modifier(DIVIDE);
    if (op == BinaryOp.DIV) return modifier(TIMES);

    // omit others since not encountered
    return null;
  }

  @Override
  public String toString() {
    return type + Arrays.toString(args);
  }
}
