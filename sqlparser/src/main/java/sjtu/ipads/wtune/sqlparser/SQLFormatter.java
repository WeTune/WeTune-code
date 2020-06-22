package sjtu.ipads.wtune.sqlparser;

import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.*;

public class SQLFormatter extends SQLVisitorAdapter {
  public static boolean DEFAULT_ONE_LINE = true;
  private static final String INDENT_STR = "  ";

  private final StringBuilder builder = new StringBuilder();
  private final boolean oneLine;

  private int indent = 0;

  public SQLFormatter() {
    this(DEFAULT_ONE_LINE);
  }

  public SQLFormatter(boolean oneLine) {
    this.oneLine = oneLine;
  }

  private void breakLine0() {
    builder.append('\n');
  }

  private void increaseIndent() {
    if (!oneLine) ++indent;
  }

  private void decreaseIndent() {
    if (!oneLine) --indent;
  }

  private void insertIndent() {
    builder.append(INDENT_STR.repeat(indent));
  }

  private void breakLine() {
    if (!oneLine) {
      breakLine0();
      insertIndent();
    } else builder.append(' ');
  }

  @Override
  public boolean enterCreateTable(SQLNode createTable) {
    builder.append("CREATE TABLE ");

    safeVisit(createTable.get(CREATE_TABLE_NAME));

    builder.append(" (");
    increaseIndent();

    for (var colDef : createTable.get(CREATE_TABLE_COLUMNS)) {
      breakLine();
      safeVisit(colDef);
      builder.append(',');
    }

    for (var conDef : createTable.get(CREATE_TABLE_CONSTRAINTS)) {
      breakLine();
      safeVisit(conDef);
      builder.append(',');
    }

    builder.deleteCharAt(builder.length() - 1);
    decreaseIndent();
    breakLine();
    insertIndent();
    builder.append(')');

    final String engine = createTable.get(CREATE_TABLE_ENGINE);
    if (engine != null) builder.append(" ENGINE = '").append(engine).append('\'');
    return false;
  }

  @Override
  public boolean enterTableName(SQLNode tableName) {
    final var schema = tableName.get(TABLE_NAME_SCHEMA);
    final var table = tableName.get(TABLE_NAME_TABLE);

    if (schema != null) builder.append('`').append(schema).append('`').append('.');
    builder.append('`').append(table).append('`');

    return false;
  }

  @Override
  public boolean enterColumnDef(SQLNode colDef) {
    safeVisit(colDef.get(COLUMN_DEF_NAME));
    builder.append(' ').append(colDef.get(COLUMN_DEF_DATATYPE_RAW));

    if (colDef.isFlagged(COLUMN_DEF_CONS, UNIQUE)) builder.append(" UNIQUE");
    if (colDef.isFlagged(COLUMN_DEF_CONS, PRIMARY)) builder.append(" PRIMARY KEY");
    if (colDef.isFlagged(COLUMN_DEF_CONS, NOT_NULL)) builder.append(" NOT NULL");
    if (colDef.isFlagged(COLUMN_DEF_AUTOINCREMENT)) builder.append(" AUTO_INCREMENT");

    final var references = colDef.get(COLUMN_DEF_REF);
    if (references != null) safeVisit(references);

    return false;
  }

  @Override
  public boolean enterColumnName(SQLNode colName) {
    final var schema = colName.get(COLUMN_NAME_SCHEMA);
    final var table = colName.get(COLUMN_NAME_TABLE);
    final var column = colName.get(COLUMN_NAME_COLUMN);

    if (schema != null) builder.append('`').append(schema).append('`').append('.');
    if (table != null) builder.append('`').append(table).append('`').append('.');
    builder.append('`').append(column).append('`');

    return false;
  }

  @Override
  public boolean enterReferences(SQLNode ref) {
    builder.append(" REFERENCES ");
    safeVisit(ref.get(REFERENCES_TABLE));

    final var columns = ref.get(REFERENCES_COLUMNS);
    if (columns != null) {
      try (final var ignored = withParen(true)) {
        for (SQLNode column : columns) {
          safeVisit(column);
          builder.append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
      }
    }

    return false;
  }

  @Override
  public boolean enterIndexDef(SQLNode indexDef) {
    final var constraint = indexDef.get(INDEX_DEF_CONS);
    final var type = indexDef.get(INDEX_DEF_TYPE);
    final var name = indexDef.get(INDEX_DEF_NAME);
    final var keys = indexDef.get(INDEX_DEF_KEYS);
    final var refs = indexDef.get(INDEX_DEF_REFS);

    if (constraint != null)
      switch (constraint) {
        case PRIMARY:
          builder.append("PRIMARY ");
          break;
        case UNIQUE:
          builder.append("UNIQUE ");
          break;
        case FOREIGN:
          builder.append("FOREIGN ");
          break;
      }

    if (type != null)
      switch (type) {
        case FULLTEXT:
          builder.append("FULLTEXT ");
          break;
        case SPATIAL:
          builder.append("SPATIAL ");
          break;
      }

    builder.append("KEY ");

    if (name != null) builder.append('`').append(name).append('`');

    try (final var ignored = withParen(true)) {
      for (SQLNode key : keys) {
        safeVisit(key);
        builder.append(", ");
      }
      builder.delete(builder.length() - 2, builder.length());
    }

    if (refs != null) safeVisit(refs);

    if (type != null)
      switch (type) {
        case BTREE:
          builder.append(" USING BTREE ");
          break;
        case RTREE:
          builder.append(" USING RTREE ");
          break;
        case HASH:
          builder.append(" USING HASH ");
          break;
      }

    return false;
  }

  @Override
  public boolean enterKeyPart(SQLNode keyPart) {
    final String columnName = keyPart.get(KEY_PART_COLUMN);
    final Integer length = keyPart.get(KEY_PART_LEN);
    final KeyDirection direction = keyPart.get(KEY_PART_DIRECTION);
    final SQLNode expr = keyPart.get(KEY_PART_EXPR);

    if (columnName != null) builder.append('`').append(columnName).append('`');
    if (length != null) builder.append('(').append(length).append(')');
    if (direction != null) builder.append(' ').append(direction);
    if (expr != null)
      try (final var ignored = withParen(true)) {
        safeVisit(expr);
      }

    return false;
  }

  @Override
  public boolean enterVariable(SQLNode variable) {
    builder.append(variable.get(VARIABLE_SCOPE).prefix());
    builder.append(variable.get(VARIABLE_NAME));

    final SQLNode assignment = variable.get(VARIABLE_ASSIGNMENT);
    if (assignment != null) {
      builder.append(" = ");
      safeVisit(assignment);
    }

    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    final Object value = literal.get(LITERAL_VALUE);
    switch (literal.get(LITERAL_TYPE)) {
      case TEXT:
        builder.append('\'').append(value).append('\'');
        break;
      case INTEGER:
      case LONG:
      case FRACTIONAL:
      case HEX:
        builder.append(value);
        break;
      case BOOL:
        builder.append(value.toString().toUpperCase());
        break;
      case NULL:
        builder.append("NULL");
        break;
      case UNKNOWN:
        builder.append("UNKNOWN");
        break;
      case TEMPORAL:
        builder
            .append(literal.get(LITERAL_UNIT).toUpperCase())
            .append(" '")
            .append(value)
            .append('\'');
    }

    return false;
  }

  @Override
  public boolean enterFuncCall(SQLNode funcCall) {
    final String name = funcCall.get(FUNC_CALL_NAME);
    final List<SQLNode> args = funcCall.getOr(FUNC_CALL_ARGS, emptyList());

    if ("extract".equalsIgnoreCase(name)) {
      builder.append("EXTRACT");
      try (final var ignored = withParen(true)) {
        if (args.size() != 2) builder.append("<??>");
        else {
          safeVisit(args.get(0));
          builder.append(" FROM ");
          safeVisit(args.get(1));
        }
      }
      return false;
    }

    if ("position".equalsIgnoreCase(name)) {
      builder.append("POSITION");
      try (final var ignored = withParen(true)) {
        if (args.size() != 2) builder.append("<??>");
        else {
          safeVisit(args.get(0));
          builder.append(" IN ");
          safeVisit(args.get(1));
        }
      }
      return false;
    }

    if ("trim".equalsIgnoreCase(name)) {
      builder.append("TRIM");
      try (final var ignored = withParen(true)) {
        if (args.size() != 3) builder.append("<??>");
        else {
          final SQLNode arg0 = args.get(0);
          final SQLNode arg1 = args.get(1);
          final SQLNode arg2 = args.get(2);

          if (arg0 != null) {
            arg0.accept(this);
            builder.append(' ');
          }
          if (arg1 != null) arg1.accept(this);
          if (arg2 != null) {
            if (arg1 != null) builder.append(' ');
            builder.append("FROM ");
            arg2.accept(this);
          }
        }
      }
      return false;
    }

    builder.append(name.toUpperCase());

    appendList(args);
    return false;
  }

  @Override
  public boolean enterCollation(SQLNode collation) {
    final SQLNode expr = collation.get(COLLATE_EXPR);
    try (final var ignored = withParen(needParen(collation, expr, true))) {
      safeVisit(expr);
    }
    builder.append(" COLLATE '").append(collation.get(COLLATE_COLLATION)).append('\'');
    return false;
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    builder.append('?');
    return false;
  }

  @Override
  public boolean enterUnary(SQLNode unary) {
    final String op = unary.get(UNARY_OP).text();
    builder.append(op);
    if (op.length() > 1) builder.append(' ');

    final SQLNode expr = unary.get(UNARY_EXPR);
    try (final var ignored = withParen(needParen(unary, expr, false))) {
      safeVisit(expr);
    }

    return false;
  }

  @Override
  public boolean enterGroupingOp(SQLNode groupingOp) {
    builder.append("GROUPING");
    appendList(groupingOp.getOr(GROUPING_OP_EXPRS, emptyList()));
    return false;
  }

  @Override
  public boolean enterTuple(SQLNode tuple) {
    if (tuple.isFlagged(TUPLE_AS_ROW)) builder.append("ROW");
    appendList(tuple.getOr(TUPLE_EXPRS, emptyList()));
    return false;
  }

  @Override
  public boolean enterMatch(SQLNode match) {
    builder.append("MATCH ");
    appendList(match.get(MATCH_COLS), false);

    builder.append(" AGAINST (");
    safeVisit(match.get(MATCH_EXPR));

    final MatchOption option = match.get(MATCH_OPTION);
    if (option != null) builder.append(' ').append(option.optionText());
    builder.append(')');

    return false;
  }

  @Override
  public boolean enterCast(SQLNode cast) {
    builder.append("CAST(");

    safeVisit(cast.get(CAST_EXPR));

    builder.append(" AS ").append(cast.get(CAST_TYPE));
    if (cast.isFlagged(CAST_IS_ARRAY)) builder.append(" ARRAY");
    builder.append(')');

    return false;
  }

  @Override
  public boolean enterDefault(SQLNode _default) {
    builder.append("DEFAULT(");
    safeVisit(_default.get(DEFAULT_COL));
    builder.append(')');
    return false;
  }

  @Override
  public boolean enterValues(SQLNode values) {
    builder.append("VALUES(");
    safeVisit(values.get(VALUES_EXPR));
    builder.append(')');
    return false;
  }

  @Override
  public boolean enterSymbol(SQLNode symbol) {
    builder.append(symbol.get(SYMBOL_TEXT).toUpperCase());
    return false;
  }

  @Override
  public boolean enterInterval(SQLNode interval) {
    builder.append("INTERVAL ");
    final SQLNode expr = interval.get(INTERVAL_EXPR);
    try (final var ignored = withParen(needParen(interval, expr, false))) {
      safeVisit(expr);
    }
    builder.append(' ').append(interval.get(INTERVAL_UNIT));

    return false;
  }

  @Override
  public boolean enterWildcard(SQLNode wildcard) {
    builder.append('*');
    return false;
  }

  @Override
  public boolean enterAggregate(SQLNode aggregate) {
    builder.append(aggregate.get(AGGREGATE_NAME).toUpperCase()).append('(');
    if (aggregate.isFlagged(AGGREGATE_DISTINCT)) builder.append("DISTINCT ");

    appendList(aggregate.get(AGGREGATE_ARGS), false);
    final List<SQLNode> order = aggregate.get(AGGREGATE_ORDER);

    if (order != null && !order.isEmpty()) {
      builder.append(" ORDER BY ");
      appendList(order, false);
    }

    final String sep = aggregate.get(AGGREGATE_SEP);
    if (sep != null) builder.append(" SEPARATOR '").append(sep).append('\'');

    builder.append(')');

    final String windowName = aggregate.get(AGGREGATE_WINDOW_NAME);
    final SQLNode windowSpec = aggregate.get(AGGREGATE_WINDOW_SPEC);
    if (windowName != null) builder.append(" OVER `").append(windowName).append('`');
    if (windowSpec != null) {
      builder.append(" OVER ");
      safeVisit(windowSpec);
    }
    return false;
  }

  @Override
  public boolean enterExists(SQLNode exists) {
    builder.append("EXISTS (");
    insertIndent();
    breakLine();
    safeVisit(exists.get(EXISTS_SUBQUERY));
    decreaseIndent();
    breakLine();
    builder.append(')');
    return false;
  }

  @Override
  public boolean enterConvertUsing(SQLNode convertUsing) {
    builder.append("CONVERT(");
    safeVisit(convertUsing.get(CONVERT_USING_EXPR));
    builder.append(" USING ");

    String charset = convertUsing.get(CONVERT_USING_CHARSET).get(SYMBOL_TEXT);
    if (!charset.equalsIgnoreCase("binary") && !charset.equalsIgnoreCase("default"))
      charset = "'" + charset + "'";

    builder.append(charset);
    builder.append(')');
    return false;
  }

  @Override
  public boolean enterCase(SQLNode _case) {
    builder.append("CASE");
    final SQLNode cond = _case.get(CASE_COND);
    if (cond != null) {
      builder.append(' ');
      try (final var ignored = withParen(needParen(_case, cond, false))) {
        safeVisit(cond);
      }
    }

    increaseIndent();
    for (SQLNode when : _case.get(CASE_WHENS)) {
      breakLine();
      safeVisit(when);
    }

    final SQLNode _else = _case.get(CASE_ELSE);
    if (_else != null) {
      breakLine();
      builder.append("ELSE ");
      try (final var ignored = withParen(needParen(_case, _else, false))) {
        safeVisit(_else);
      }
    }

    decreaseIndent();
    breakLine();
    builder.append("END");

    return false;
  }

  @Override
  public boolean enterWhen(SQLNode when) {
    builder.append("WHEN ");
    final SQLNode cond = when.get(WHEN_COND);
    try (final var ignored = withParen(needParen(when, cond, false))) {
      safeVisit(cond);
    }

    builder.append(" THEN ");
    final SQLNode expr = when.get(WHEN_EXPR);
    try (final var ignored = withParen(needParen(when, expr, false))) {
      safeVisit(expr);
    }
    return false;
  }

  @Override
  public boolean enterWindowFrame(SQLNode windowFrame) {
    builder.append(windowFrame.get(WINDOW_FRAME_UNIT)).append(' ');
    final SQLNode start = windowFrame.get(WINDOW_FRAME_START);
    final SQLNode end = windowFrame.get(WINDOW_FRAME_END);

    if (end != null) builder.append("BETWEEN ");
    safeVisit(start);
    if (end != null) {
      builder.append(" AND ");
      safeVisit(end);
    }

    final WindowExclusion exclusion = windowFrame.get(WINDOW_FRAME_EXCLUSION);
    if (exclusion != null) builder.append(" EXCLUDE ").append(exclusion.text());

    return false;
  }

  @Override
  public boolean enterFrameBound(SQLNode frameBound) {
    if (frameBound.get(FRAME_BOUND_DIRECTION) == null) builder.append("CURRENT ROW");
    else {
      safeVisit(frameBound.get(FRAME_BOUND_EXPR));
      builder.append(' ').append(frameBound.get(FRAME_BOUND_DIRECTION));
    }

    return false;
  }

  @Override
  public boolean enterWindowSpec(SQLNode windowSpec) {
    builder.append("(");

    final String name = windowSpec.get(WINDOW_SPEC_NAME);
    if (name != null) builder.append('`').append(name).append('`');

    final List<SQLNode> partition = windowSpec.get(WINDOW_SPEC_PARTITION);
    if (partition != null && !partition.isEmpty()) {
      builder.append(" PARTITION BY ");
      appendList(partition, false);
    }

    final List<SQLNode> order = windowSpec.get(WINDOW_SPEC_ORDER);
    if (order != null && !order.isEmpty()) {
      builder.append(" ORDER BY ");
      appendList(order, false);
    }

    final SQLNode frame = windowSpec.get(WINDOW_SPEC_FRAME);
    if (frame != null) {
      builder.append(' ');
      safeVisit(frame);
    }

    builder.append(')');

    return false;
  }

  @Override
  public boolean enterBinary(SQLNode binary) {
    final SQLNode left = binary.get(BINARY_LEFT);
    try (final var ignored = withParen(needParen(binary, left, true))) {
      safeVisit(left);
    }

    final BinaryOp op = binary.get(BINARY_OP);

    builder.append(' ').append(op.text()).append(' ');

    try (final var ignored0 = withParen(op == BinaryOp.MEMBER_OF)) {
      final SQLNode right = binary.get(BINARY_RIGHT);
      try (final var ignored1 = withParen(needParen(binary, right, false))) {
        safeVisit(right);
      }
    }

    return false;
  }

  @Override
  public boolean enterTernary(SQLNode ternary) {
    final TernaryOp operator = ternary.get(TERNARY_OP);
    final SQLNode left = ternary.get(TERNARY_LEFT);
    final SQLNode middle = ternary.get(TERNARY_MIDDLE);
    final SQLNode right = ternary.get(TERNARY_RIGHT);

    try (final var ignored = withParen(needParen(ternary, left, false))) {
      safeVisit(left);
    }

    builder.append(' ').append(operator.text0()).append(' ');
    try (final var ignored = withParen(needParen(ternary, middle, false))) {
      safeVisit(middle);
    }

    builder.append(' ').append(operator.text1()).append(' ');
    try (final var ignored = withParen(needParen(ternary, right, false))) {
      safeVisit(right);
    }

    return false;
  }

  @Override
  public boolean enterOrderItem(SQLNode orderItem) {
    safeVisit(orderItem.get(ORDER_ITEM_EXPR));
    final KeyDirection direction = orderItem.get(ORDER_ITEM_DIRECTION);
    if (direction != null) builder.append(' ').append(direction.name());
    return false;
  }

  private DumbAutoCloseable withParen(boolean addParen) {
    return addParen ? new ParenCtx() : DumbAutoCloseable.INSTANCE;
  }

  private static class DumbAutoCloseable implements AutoCloseable {
    private static final DumbAutoCloseable INSTANCE = new DumbAutoCloseable();

    @Override
    public void close() {}
  }

  private class ParenCtx extends DumbAutoCloseable {
    ParenCtx() {
      builder.append('(');
    }

    @Override
    public void close() {
      builder.append(')');
    }
  }

  private void safeVisit(SQLNode node) {
    if (node == null) builder.append("<??>");
    else node.accept(this);
  }

  private void appendList(List<SQLNode> exprs, boolean withParen) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) builder.append("()");
      return;
    }

    if (withParen) builder.append('(');

    for (SQLNode arg : exprs) {
      if (arg == null) continue;
      arg.accept(this);
      builder.append(", ");
    }

    builder.deleteCharAt(builder.length() - 1);
    builder.deleteCharAt(builder.length() - 1);

    if (withParen) builder.append(')');
  }

  private void appendList(List<SQLNode> exprs) {
    appendList(exprs, true);
  }

  private static boolean needParen(SQLNode parent, SQLNode child, boolean isLeftChild) {
    if (parent == null || child == null) return false;

    final int parentPrecedence = getOperatorPrecedence(parent);
    final int childPrecedence = getOperatorPrecedence(child);
    if (parentPrecedence == -1 || childPrecedence == -1) return false;
    if (parentPrecedence > childPrecedence) return true;
    if (parentPrecedence == childPrecedence) return !isLeftChild;
    return false;
  }

  public String toString() {
    return builder.toString();
  }
}
