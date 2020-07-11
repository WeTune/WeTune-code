package sjtu.ipads.wtune.stmt.attrs;

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
    CHECK_BOOL,
    NEQ,
    COLUMN_VALUE,
    INVOKE_FUNC,
    INVOKE_AGG,
    DIRECT_VALUE,
    MAKE_TUPLE,
    MATCHING,
    GEN_OFFSET,
    GUESS,
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

  public static ParamModifier of(Type type, Object... args) {
    return new ParamModifier(type, args);
  }
}
