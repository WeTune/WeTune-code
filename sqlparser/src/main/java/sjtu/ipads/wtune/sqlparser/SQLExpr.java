package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.attrs.Attrs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.attrs.Attrs.Key.checkEquals;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.Kind.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;

public class SQLExpr {
  public enum Kind {
    UNKNOWN,
    VARIABLE,
    COLUMN_REF,
    FUNC_CALL,
    COLLATE,
    INTERVAL,
    SYMBOL,
    LITERAL,
    PARAM_MARKER,
    AGGREGATE,
    WILDCARD,
    GROUPING_OP,
    UNARY,
    BINARY,
    TERNARY,
    TUPLE,
    EXISTS,
    MATCH,
    CAST,
    CASE,
    WHEN,
    CONVERT_USING,
    DEFAULT,
    VALUES,
    QUERY_EXPR;

    private final Set<String> attrs = new HashSet<>();

    public void addAttr(String attr) {
      attrs.add(attr);
    }
  }

  public enum VariableScope {
    USER("@"),
    SYSTEM_GLOBAL("@@GLOBAL."),
    SYSTEM_LOCAL("@@LOCAL."),
    SYSTEM_SESSION("@@SESSION.");
    private final String prefix;

    VariableScope(String prefix) {
      this.prefix = prefix;
    }

    public String prefix() {
      return prefix;
    }
  }

  public enum LiteralType {
    TEXT,
    INTEGER,
    LONG,
    FRACTIONAL,
    TEMPORAL,
    NULL,
    BOOL,
    HEX,
    UNKNOWN
  }

  public enum IntervalUnit {
    MICROSECOND,
    SECOND,
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
    SECOND_MICROSECOND,
    MINUTE_MICROSECOND,
    MINUTE_SECOND,
    HOUR_MICROSECOND,
    HOUR_SECOND,
    HOUR_MINUTE,
    DAY_MICROSECOND,
    DAY_SECOND,
    DAY_MINUTE,
    DAY_HOUR,
    YEAR_MONTH;
  }

  public enum MatchOption {
    BOOLEAN_MODE("in boolean mode"),
    NATURAL_MODE("in natural language mode"),
    NATURAL_MODE_WITH_EXPANSION("in natural language mode with query expansion"),
    WITH_EXPANSION("with query expansion");

    private final String optionText;

    MatchOption(String optionText) {
      this.optionText = optionText.toUpperCase();
    }

    public String optionText() {
      return optionText;
    }
  }

  public enum UnaryOp {
    NOT("NOT", 4),
    BINARY("BINARY", 13),
    UNARY_PLUS("+", 12),
    UNARY_MINUS("-", 12),
    UNARY_FLIP("~", 12);

    private final String text;
    private final int precedence;

    UnaryOp(String text, int precedence) {
      this.text = text;
      this.precedence = precedence;
    }

    public String text() {
      return text;
    }

    public static UnaryOp ofOp(String text) {
      for (UnaryOp value : values()) if (value.text().equalsIgnoreCase(text)) return value;
      return null;
    }

    public int precedence() {
      return precedence;
    }

    public boolean isLogic() {
      return this == NOT;
    }
  }

  public enum BinaryOp {
    BITWISE_XOR("^", 12),
    MULT("*", 11),
    DIV("/", 11),
    MOD("%", 11),
    PLUS("+", 10),
    MINUS("-", 10),
    LEFT_SHIFT("<<", 9),
    RIGHT_SHIFT(">>", 9),
    BITWISE_AND("&", 8),
    BITWISE_OR("|", 7),
    EQUAL("=", 6),
    IS("IS", 6),
    NULL_SAFE_EQUAL("<=>", 6),
    GREATER_OR_EQUAL(">=", 6),
    GREATER_THAN(">", 6),
    LESS_OR_EQUAL("<=", 6),
    LESS_THAN("<", 6),
    NOT_EQUAL("<>", 6),
    IN_LIST("IN", 6),
    IN_SUBQUERY("IN", 6),
    LIKE("LIKE", 6),
    REGEXP("REGEXP", 6),
    MEMBER_OF("MEMBER OF", 6),
    SOUNDS_LIKE("SOUNDS LIKE", 6),
    AND("AND", 3),
    XOR_SYMBOL("XOR", 2),
    OR("OR", 1);

    private final String text;
    private final int precedence;

    BinaryOp(String text, int precedence) {
      this.text = text.toUpperCase();
      this.precedence = precedence < 0 ? Integer.MAX_VALUE : precedence;
    }

    public String text() {
      return text;
    }

    public static BinaryOp ofOp(String opText) {
      if (opText.equalsIgnoreCase("DIV")) return DIV;
      if (opText.equalsIgnoreCase("MOD")) return MOD;
      if (opText.equalsIgnoreCase("!=")) return NOT_EQUAL;
      for (BinaryOp op : values()) if (op.text.equals(opText)) return op;
      return null;
    }

    public boolean isArithmetic() {
      return precedence >= BITWISE_AND.precedence;
    }

    public boolean isRelation() {
      return precedence == LIKE.precedence;
    }

    public boolean isLogic() {
      return precedence <= AND.precedence;
    }

    public int precedence() {
      return precedence;
    }
  }

  public enum TernaryOp {
    BETWEEN_AND("BETWEEN", "AND", 4);

    private String text0;
    private String text1;
    private int precedence;

    TernaryOp(String text0, String text1, int precedence) {
      this.text0 = text0;
      this.text1 = text1;
      this.precedence = precedence;
    }

    public String text0() {
      return text0;
    }

    public String text1() {
      return text1;
    }

    public int precedence() {
      return precedence;
    }
  }

  public enum SubqueryOption {
    ANY,
    ALL
  }

  public static final Attrs.Key<Kind> EXPR_KIND = Attrs.key(SQL_ATTR_PREFIX + ".expr.kind", Kind.class);

  public static SQLNode newExpr(Kind kind) {
    final SQLNode node = new SQLNode(SQLNode.Type.EXPR);
    node.put(EXPR_KIND, kind == null ? UNKNOWN : kind);
    return node;
  }

  public static SQLNode paramMarker() {
    return newExpr(Kind.PARAM_MARKER);
  }

  public static SQLNode symbol(String text) {
    final SQLNode node = newExpr(Kind.SYMBOL);
    node.put(SYMBOL_TEXT, text);
    return node;
  }

  public static SQLNode literal(LiteralType type, Object value) {
    final SQLNode node = newExpr(Kind.LITERAL);
    node.put(LITERAL_TYPE, type);
    if (value != null) node.put(LITERAL_VALUE, value);
    return node;
  }

  public static SQLNode wildcard() {
    return newExpr(Kind.WILDCARD);
  }

  public static SQLNode wildcard(SQLNode table) {
    final SQLNode node = newExpr(WILDCARD);
    node.put(WILDCARD_TABLE, table);
    return node;
  }

  public static SQLNode unary(SQLNode expr, UnaryOp op) {
    final SQLNode node = newExpr(UNARY);
    node.put(UNARY_EXPR, expr);
    node.put(UNARY_OP, op);
    return node;
  }

  public static SQLNode binary(SQLNode left, SQLNode right, BinaryOp op) {
    final SQLNode binary = newExpr(BINARY);
    binary.put(BINARY_LEFT, left);
    binary.put(BINARY_RIGHT, right);
    binary.put(BINARY_OP, op);
    return binary;
  }

  public static SQLNode columnRef(String tableName, String columnName) {
    final SQLNode columnId = new SQLNode(SQLNode.Type.COLUMN_NAME);
    columnId.put(COLUMN_NAME_TABLE, tableName);
    columnId.put(COLUMN_NAME_COLUMN, columnName);

    final SQLNode columnRef = newExpr(COLUMN_REF);
    columnRef.put(COLUMN_REF_COLUMN, columnId);
    return columnRef;
  }

  public static boolean isExpr(SQLNode node) {
    return node.type() == SQLNode.Type.EXPR;
  }

  public static Kind exprKind(SQLNode node) {
    return node.get(EXPR_KIND);
  }

  public static int getOperatorPrecedence(SQLNode node) {
    if (!isExpr(node)) return -1;
    final Kind kind = node.get(EXPR_KIND);
    switch (kind) {
      case UNARY:
        return node.get(UNARY_OP).precedence();
      case BINARY:
        return node.get(BINARY_OP).precedence();
      case TERNARY:
        return node.get(TERNARY_OP).precedence();
      case CASE:
      case WHEN:
        return 5;
      case COLLATE:
        return 13;
      case INTERVAL:
        return 14;
      default:
        return -1;
    }
  }

  static final String EXPR_ATTR_PREFIX = SQL_ATTR_PREFIX + ".expr.";

  private static String attrPrefix(Kind kind) {
    return EXPR_ATTR_PREFIX + kind.name().toLowerCase() + ".";
  }

  private static <T> Attrs.Key<T> attr(Kind kind, String name, Class<T> clazz) {
    final Attrs.Key<T> attr = Attrs.key(attrPrefix(kind) + name, clazz);
    attr.addCheck(checkEquals(EXPR_KIND, kind));
    kind.addAttr(attr.name());
    return attr;
  }

  private static <T> Attrs.Key<T> attr2(Kind kind, String name, Class<?> clazz) {
    final Attrs.Key<T> attr = Attrs.key2(attrPrefix(kind) + name, clazz);
    attr.addCheck(checkEquals(EXPR_KIND, kind));
    kind.addAttr(attr.name());
    return attr;
  }

  private static Attrs.Key<String> stringAttr(Kind kind, String name) {
    return attr(kind, name, String.class);
  }

  private static Attrs.Key<Boolean> booleanAttr(Kind kind, String name) {
    return attr(kind, name, Boolean.class);
  }

  private static Attrs.Key<SQLNode> nodeAttr(Kind kind, String name) {
    return attr(kind, name, SQLNode.class);
  }

  private static Attrs.Key<List<SQLNode>> nodesAttr(Kind kind, String name) {
    return attr2(kind, name, List.class);
  }

  // Unknown
  public static final Attrs.Key<Object> EXPR_UNKNOWN_RAW = attr(UNKNOWN, "raw", Object.class);

  // Variable
  public static final Attrs.Key<VariableScope> VARIABLE_SCOPE =
      attr(VARIABLE, "scope", VariableScope.class);
  public static final Attrs.Key<String> VARIABLE_NAME = stringAttr(VARIABLE, "name");
  public static final Attrs.Key<SQLNode> VARIABLE_ASSIGNMENT = nodeAttr(VARIABLE, "assignment");

  // Col Ref
  public static final Attrs.Key<SQLNode> COLUMN_REF_COLUMN = nodeAttr(COLUMN_REF, "column");

  // Func Call
  public static final Attrs.Key<String> FUNC_CALL_NAME = stringAttr(FUNC_CALL, "name");
  public static final Attrs.Key<List<SQLNode>> FUNC_CALL_ARGS = nodesAttr(FUNC_CALL, "args");

  // Collate
  public static final Attrs.Key<SQLNode> COLLATE_EXPR = nodeAttr(COLLATE, "expr");
  public static final Attrs.Key<String> COLLATE_COLLATION = stringAttr(COLLATE, "collation");

  // Interval
  public static final Attrs.Key<SQLNode> INTERVAL_EXPR = nodeAttr(INTERVAL, "expr");
  public static final Attrs.Key<IntervalUnit> INTERVAL_UNIT =
      attr(INTERVAL, "unit", IntervalUnit.class);

  // Symbol
  public static final Attrs.Key<String> SYMBOL_TEXT = stringAttr(SYMBOL, "text");

  // Literal
  public static final Attrs.Key<LiteralType> LITERAL_TYPE =
      attr(LITERAL, "type", LiteralType.class);
  public static final Attrs.Key<Object> LITERAL_VALUE = attr(LITERAL, "value", Object.class);
  public static final Attrs.Key<String> LITERAL_UNIT = stringAttr(LITERAL, "unit");

  // Aggregate
  public static final Attrs.Key<String> AGGREGATE_NAME = stringAttr(AGGREGATE, "name");
  public static final Attrs.Key<Boolean> AGGREGATE_DISTINCT = booleanAttr(AGGREGATE, "distinct");
  public static final Attrs.Key<List<SQLNode>> AGGREGATE_ARGS = nodesAttr(AGGREGATE, "args");
  public static final Attrs.Key<String> AGGREGATE_WINDOW_NAME = stringAttr(AGGREGATE, "windowName");
  public static final Attrs.Key<SQLNode> AGGREGATE_WINDOW_SPEC = nodeAttr(AGGREGATE, "windowSpec");
  /* GROUP_CONCAT only */
  public static final Attrs.Key<List<SQLNode>> AGGREGATE_ORDER = nodesAttr(AGGREGATE, "order");
  /* GROUP_CONCAT only */
  public static final Attrs.Key<String> AGGREGATE_SEP = stringAttr(AGGREGATE, "sep");

  // Wildcard
  public static final Attrs.Key<SQLNode> WILDCARD_TABLE = nodeAttr(WILDCARD, "table");

  // Grouping
  public static final Attrs.Key<List<SQLNode>> GROUPING_OP_EXPRS = nodesAttr(GROUPING_OP, "exprs");

  // Unary
  public static final Attrs.Key<UnaryOp> UNARY_OP = attr(UNARY, "op", UnaryOp.class);
  public static final Attrs.Key<SQLNode> UNARY_EXPR = nodeAttr(UNARY, "expr");

  // Binary
  public static final Attrs.Key<BinaryOp> BINARY_OP = attr(BINARY, "op", BinaryOp.class);
  public static final Attrs.Key<SQLNode> BINARY_LEFT = nodeAttr(BINARY, "left");
  public static final Attrs.Key<SQLNode> BINARY_RIGHT = nodeAttr(BINARY, "right");
  public static final Attrs.Key<SubqueryOption> BINARY_SUBQUERY_OPTION =
      attr(BINARY, "right", SubqueryOption.class);

  // TERNARY
  public static final Attrs.Key<TernaryOp> TERNARY_OP = attr(TERNARY, "op", TernaryOp.class);
  public static final Attrs.Key<SQLNode> TERNARY_LEFT = nodeAttr(TERNARY, "left");
  public static final Attrs.Key<SQLNode> TERNARY_MIDDLE = nodeAttr(TERNARY, "middle");
  public static final Attrs.Key<SQLNode> TERNARY_RIGHT = nodeAttr(TERNARY, "right");

  // Tuple
  public static final Attrs.Key<List<SQLNode>> TUPLE_EXPRS = nodesAttr(TUPLE, "exprs");
  public static final Attrs.Key<Boolean> TUPLE_AS_ROW = booleanAttr(TUPLE, "asRow");

  // Exists
  public static final Attrs.Key<SQLNode> EXISTS_SUBQUERY = nodeAttr(EXISTS, "subquery");

  // MatchAgainst
  public static final Attrs.Key<List<SQLNode>> MATCH_COLS = nodesAttr(MATCH, "columns");
  public static final Attrs.Key<SQLNode> MATCH_EXPR = nodeAttr(MATCH, "expr");
  public static final Attrs.Key<MatchOption> MATCH_OPTION =
      attr(MATCH, "option", MatchOption.class);

  // Cast
  public static final Attrs.Key<SQLNode> CAST_EXPR = nodeAttr(CAST, "expr");
  public static final Attrs.Key<SQLNode> CAST_TYPE = nodeAttr(CAST, "type");
  public static final Attrs.Key<Boolean> CAST_IS_ARRAY = booleanAttr(CAST, "isArray");

  // Case
  public static final Attrs.Key<SQLNode> CASE_COND = nodeAttr(CASE, "condition");
  public static final Attrs.Key<List<SQLNode>> CASE_WHENS = nodesAttr(CASE, "when");
  public static final Attrs.Key<SQLNode> CASE_ELSE = nodeAttr(CASE, "else");

  // When
  public static final Attrs.Key<SQLNode> WHEN_COND = nodeAttr(WHEN, "condition");
  public static final Attrs.Key<SQLNode> WHEN_EXPR = nodeAttr(WHEN, "expr");

  // ConvertUsing
  public static final Attrs.Key<SQLNode> CONVERT_USING_EXPR = nodeAttr(CONVERT_USING, "expr");
  public static final Attrs.Key<SQLNode> CONVERT_USING_CHARSET = nodeAttr(CONVERT_USING, "charset");

  // Default
  public static final Attrs.Key<SQLNode> DEFAULT_COL = nodeAttr(DEFAULT, "col");

  // Values
  public static final Attrs.Key<SQLNode> VALUES_EXPR = nodeAttr(VALUES, "expr");

  // QueryExpr
  public static final Attrs.Key<SQLNode> QUERY_EXPR_QUERY = nodeAttr(QUERY_EXPR, "query");
}
