package sjtu.ipads.wtune.sqlparser;

import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.trimTrailing;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;

public class SQLFormatter implements SQLVisitor {
  public static boolean DEFAULT_ONE_LINE = true;
  private static final String INDENT_STR = "  ";
  private static final String UNKNOWN_PLACEHOLDER = "<??>";

  private final StringBuilder builder = new StringBuilder();
  private final boolean oneLine;

  private int indent = 0;

  public SQLFormatter() {
    this(DEFAULT_ONE_LINE);
  }

  public SQLFormatter(boolean oneLine) {
    this.oneLine = oneLine;
  }

  private static char quotation(SQLNode node) {
    if (POSTGRESQL.equals(node.dbType())) {
      return '`';
    } else {
      return '"';
    }
  }

  private static String quotation2(SQLNode node) {
    if (POSTGRESQL.equals(node.dbType())) {
      return "`";
    } else {
      return "\"";
    }
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

  private void breakLine(boolean spaceIfOneLine) {
    if (!oneLine) {
      breakLine0();
      insertIndent();
    } else if (spaceIfOneLine) builder.append(' ');
  }

  private void breakLine() {
    breakLine(true);
  }

  private void appendName(SQLNode node, String name, boolean withDot) {
    if (name == null) return;
    builder.append(quotation(node)).append(name).append(quotation(node));
    if (withDot) builder.append('.');
  }

  @Override
  public boolean enter(SQLNode node) {
    if (node.type() == Type.INVALID) {
      builder.append(UNKNOWN_PLACEHOLDER);
      return false;
    }
    return true;
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

    trimTrailing(builder, 1);
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
    appendName(tableName, tableName.get(TABLE_NAME_SCHEMA), true);
    appendName(tableName, tableName.get(TABLE_NAME_TABLE), false);

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
    appendName(colName, colName.get(COLUMN_NAME_SCHEMA), true);
    appendName(colName, colName.get(COLUMN_NAME_TABLE), true);
    appendName(colName, colName.get(COLUMN_NAME_COLUMN), false);

    return false;
  }

  @Override
  public boolean enterCommonName(SQLNode commonName) {
    appendName(commonName, commonName.get(COMMON_NAME_0), true);
    appendName(commonName, commonName.get(COMMON_NAME_1), true);
    appendName(commonName, commonName.get(COMMON_NAME_2), false);
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

    if (name != null) builder.append(quotation(indexDef)).append(name).append(quotation(indexDef));

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

    if (columnName != null)
      builder.append(quotation(keyPart)).append(columnName).append(quotation(keyPart));
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
        if (args.size() != 2) builder.append(UNKNOWN_PLACEHOLDER);
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
        if (args.size() != 2) builder.append(UNKNOWN_PLACEHOLDER);
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
        if (args.size() != 3) builder.append(UNKNOWN_PLACEHOLDER);
        else {
          final SQLNode arg0 = args.get(0);
          final SQLNode arg1 = args.get(1);
          final SQLNode arg2 = args.get(2);

          if (arg0 != null) {
            safeVisit(arg0);
            builder.append(' ');
          }
          if (arg1 != null) safeVisit(arg1);
          if (arg2 != null) {
            if (arg1 != null) builder.append(' ');
            builder.append("FROM ");
            safeVisit(arg2);
          }
        }
      }
      return false;
    }

    builder.append(name.toUpperCase());

    appendNodes(args, true, true);
    return false;
  }

  @Override
  public boolean enterCollation(SQLNode collation) {
    final SQLNode expr = collation.get(COLLATE_EXPR);
    try (final var ignored = withParen(needParen(collation, expr, true))) {
      safeVisit(expr);
    }
    builder.append(" COLLATE ");
    if (POSTGRESQL.equals(collation.dbType())) {
      collation.get(COLLATE_COLLATION).accept(this);
    } else {
      builder.append('\'').append(collation.get(COLLATE_COLLATION).get(SYMBOL_TEXT)).append('\'');
    }
    return false;
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    if (POSTGRESQL.equals(paramMarker.dbType())) {
      builder.append('$').append(paramMarker.getOr(PARAM_MARKER_NUMBER, 1));
    } else {
      builder.append('?');
    }
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
    appendNodes(groupingOp.getOr(GROUPING_OP_EXPRS, emptyList()));
    return false;
  }

  @Override
  public boolean enterTuple(SQLNode tuple) {
    if (tuple.isFlagged(TUPLE_AS_ROW)) builder.append("ROW");
    appendNodes(tuple.getOr(TUPLE_EXPRS, emptyList()));
    return false;
  }

  @Override
  public boolean enterMatch(SQLNode match) {
    builder.append("MATCH ");
    appendNodes(match.get(MATCH_COLS), false);

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
    final SQLNode table = wildcard.get(WILDCARD_TABLE);
    if (table != null) {
      safeVisit(table);
      builder.append('.');
    }
    builder.append('*');
    return false;
  }

  @Override
  public boolean enterAggregate(SQLNode aggregate) {
    builder.append(aggregate.get(AGGREGATE_NAME).toUpperCase()).append('(');
    if (aggregate.isFlagged(AGGREGATE_DISTINCT)) builder.append("DISTINCT ");

    appendNodes(aggregate.get(AGGREGATE_ARGS), false, true);
    final List<SQLNode> order = aggregate.get(AGGREGATE_ORDER);

    if (order != null && !order.isEmpty()) {
      builder.append(" ORDER BY ");
      appendNodes(order, false);
    }

    final String sep = aggregate.get(AGGREGATE_SEP);
    if (sep != null) builder.append(" SEPARATOR '").append(sep).append('\'');

    builder.append(')');

    final String windowName = aggregate.get(AGGREGATE_WINDOW_NAME);
    final SQLNode windowSpec = aggregate.get(AGGREGATE_WINDOW_SPEC);
    if (windowName != null)
      builder
          .append(" OVER ")
          .append(quotation(aggregate))
          .append(windowName)
          .append(quotation(aggregate));
    if (windowSpec != null) {
      builder.append(" OVER ");
      safeVisit(windowSpec);
    }
    return false;
  }

  @Override
  public boolean enterExists(SQLNode exists) {
    builder.append("EXISTS ");
    try (final var ignored = withParen(true)) {
      increaseIndent();
      breakLine(false);
      safeVisit(exists.get(EXISTS_SUBQUERY));
      decreaseIndent();
      breakLine(false);
    }
    return false;
  }

  @Override
  public boolean enterJoinedTableSource(SQLNode joinedTableSource) {
    final SQLNode left = joinedTableSource.get(JOINED_LEFT);
    final SQLNode right = joinedTableSource.get(JOINED_RIGHT);
    final JoinType joinType = joinedTableSource.get(JOINED_TYPE);
    final SQLNode on = joinedTableSource.get(JOINED_ON);
    final List<String> using = joinedTableSource.get(JOINED_USING);

    safeVisit(left);

    breakLine();

    builder.append(joinType.text()).append(' ');
    final boolean needParen =
        isJoined(right) && !(joinType.isInner() && right.get(JOINED_TYPE).isInner());
    try (final var ignored = withParen(needParen)) {
      if (needParen) {
        increaseIndent();
        breakLine(false);
      }

      safeVisit(right);

      if (on != null || using != null) {
        increaseIndent();
        breakLine();
        if (on != null) {
          builder.append("ON ");
          safeVisit(on);
        } else {
          builder.append("USING ");
          appendStrings(using, true, quotation2(joinedTableSource));
        }
        decreaseIndent();
      }

      if (needParen) {
        decreaseIndent();
        breakLine(false);
      }
    }

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
    final String alias = windowSpec.get(WINDOW_SPEC_ALIAS);
    if (alias != null)
      builder
          .append(quotation(windowSpec))
          .append(alias)
          .append(quotation(windowSpec))
          .append(" AS ");

    builder.append("(");

    final String name = windowSpec.get(WINDOW_SPEC_NAME);
    if (name != null)
      builder.append(quotation(windowSpec)).append(name).append(quotation(windowSpec));

    final List<SQLNode> partition = windowSpec.get(WINDOW_SPEC_PARTITION);
    if (partition != null && !partition.isEmpty()) {
      builder.append(" PARTITION BY ");
      appendNodes(partition, false);
    }

    final List<SQLNode> order = windowSpec.get(WINDOW_SPEC_ORDER);
    if (order != null && !order.isEmpty()) {
      builder.append(" ORDER BY ");
      appendNodes(order, false);
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

    if (op.isLogic()) breakLine();
    else builder.append(' ');

    builder.append(op.text()).append(' ');

    final SQLNode right = binary.get(BINARY_RIGHT);

    final boolean needParen =
        op == BinaryOp.MEMBER_OF || right.type() == Type.QUERY || needParen(binary, right, false);
    final boolean needIndent = needParen && (op == BinaryOp.IN_SUBQUERY || op.isLogic());

    try (final var ignored0 = withParen(needParen)) {
      if (needIndent) {
        increaseIndent();
        breakLine(false);
      }

      safeVisit(right);

      if (needIndent) {
        decreaseIndent();
        breakLine(false);
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

  @Override
  public boolean enterSelectItem(SQLNode selectItem) {
    safeVisit(selectItem.get(SELECT_ITEM_EXPR));

    final String alias = selectItem.get(SELECT_ITEM_ALIAS);
    if (alias != null)
      builder
          .append(" AS ")
          .append(quotation(selectItem))
          .append(alias)
          .append(quotation(selectItem));

    return false;
  }

  @Override
  public boolean enterIndexHint(SQLNode indexHint) {
    final IndexHintType type = indexHint.get(INDEX_HINT_TYPE);
    builder.append(type).append(" INDEX");

    final IndexHintTarget target = indexHint.get(INDEX_HINT_TARGET);
    if (target != null) builder.append(" FOR ").append(target.text());

    builder.append(' ');
    try (final var ignored = withParen(true)) {
      final List<String> names = indexHint.get(INDEX_HINT_NAMES);

      if (names != null && !names.isEmpty()) {
        for (String name : names) {
          if (name.equalsIgnoreCase("primary")) builder.append("PRIMARY");
          else builder.append(quotation(indexHint)).append(name).append(quotation(indexHint));
          builder.append(", ");
        }
        trimTrailing(builder, 2);
      }
    }

    return false;
  }

  @Override
  public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
    safeVisit(simpleTableSource.get(SIMPLE_TABLE));

    final List<String> partitions = simpleTableSource.get(SIMPLE_PARTITIONS);
    if (partitions != null && !partitions.isEmpty()) {
      builder.append(" PARTITION ");
      appendStrings(partitions, true, quotation2(simpleTableSource));
    }

    final String alias = simpleTableSource.get(SIMPLE_ALIAS);
    if (alias != null)
      builder
          .append(" AS ")
          .append(quotation(simpleTableSource))
          .append(alias)
          .append(quotation(simpleTableSource));

    final List<SQLNode> hints = simpleTableSource.get(SIMPLE_HINTS);
    if (hints != null && !hints.isEmpty()) {
      builder.append(' ');
      appendNodes(hints, false);
    }

    return false;
  }

  @Override
  public boolean enterQuery(SQLNode query) {
    safeVisit(query.get(QUERY_BODY));

    final List<SQLNode> orderBy = query.get(QUERY_ORDER_BY);
    if (orderBy != null) {
      breakLine();
      builder.append("ORDER BY");
      increaseIndent();
      breakLine();
      appendNodes(orderBy, false);
      decreaseIndent();
    }

    final SQLNode offset = query.get(QUERY_OFFSET);
    final SQLNode limit = query.get(QUERY_LIMIT);

    if (limit != null) {
      breakLine();
      builder.append("LIMIT ");
      if (offset != null) {
        safeVisit(offset);
        builder.append(", ");
      }
      safeVisit(limit);
    }

    return false;
  }

  @Override
  public boolean enterQuerySpec(SQLNode querySpec) {
    final boolean distinct = querySpec.isFlagged(QUERY_SPEC_DISTINCT);
    final List<SQLNode> selectItems = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final SQLNode from = querySpec.get(QUERY_SPEC_FROM);
    final SQLNode where = querySpec.get(QUERY_SPEC_WHERE);
    final List<SQLNode> groupBy = querySpec.get(QUERY_SPEC_GROUP_BY);
    final OLAPOption olapOption = querySpec.get(QUERY_SPEC_OLAP_OPTION);
    final SQLNode having = querySpec.get(QUERY_SPEC_HAVING);
    final List<SQLNode> windows = querySpec.get(QUERY_SPEC_WINDOWS);

    builder.append("SELECT");
    if (distinct) builder.append(" DISTINCT");

    increaseIndent();
    breakLine();
    appendNodes(selectItems, false);
    decreaseIndent();

    if (from != null) {
      breakLine();
      builder.append("FROM ");
      increaseIndent();
      safeVisit(from);
      decreaseIndent();
    }

    if (where != null) {
      breakLine();
      builder.append("WHERE");
      increaseIndent();
      breakLine();
      safeVisit(where);
      decreaseIndent();
    }

    if (groupBy != null) {
      breakLine();
      builder.append("GROUP BY");
      increaseIndent();
      breakLine();
      appendNodes(groupBy, false);
      if (olapOption != null) {
        breakLine();
        builder.append(olapOption.text());
      }
      decreaseIndent();
    }

    if (having != null) {
      breakLine();
      builder.append("HAVING");
      increaseIndent();
      breakLine();
      safeVisit(having);
      decreaseIndent();
    }

    if (windows != null) {
      breakLine();
      builder.append("WINDOW");
      increaseIndent();
      breakLine();
      appendNodes(windows, false);
      decreaseIndent();
    }

    return false;
  }

  @Override
  public boolean enterQueryExpr(SQLNode queryExpr) {
    try (final var ignored = withParen(true)) {
      breakLine(false);
      safeVisit(queryExpr.get(QUERY_EXPR_QUERY));
      breakLine(false);
    }
    return false;
  }

  @Override
  public boolean enterSetOp(SQLNode setOp) {
    try (final var ignored = withParen(true)) {
      safeVisit(setOp.get(SET_OPERATION_LEFT));
    }

    breakLine();
    builder.append(setOp.get(SET_OPERATION_TYPE));

    final SetOperationOption option = setOp.get(SET_OPERATION_OPTION);
    if (option != null) builder.append(' ').append(option);
    breakLine();

    try (final var ignored = withParen(true)) {
      safeVisit(setOp.get(SET_OPERATION_RIGHT));
    }

    return false;
  }

  @Override
  public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    if (derivedTableSource.isFlagged(DERIVED_LATERAL)) builder.append("LATERAL ");
    try (final var ignored = withParen(true)) {
      increaseIndent();
      breakLine(false);
      safeVisit(derivedTableSource.get(DERIVED_SUBQUERY));
      decreaseIndent();
      breakLine(false);
    }

    final String alias = derivedTableSource.get(DERIVED_ALIAS);
    if (alias != null)
      builder
          .append(" AS ")
          .append(quotation(derivedTableSource))
          .append(alias)
          .append(quotation(derivedTableSource));

    final List<String> internalRefs = derivedTableSource.get(DERIVED_INTERNAL_REFS);
    if (internalRefs != null && !internalRefs.isEmpty()) {
      breakLine();
      appendStrings(internalRefs, true, quotation2(derivedTableSource));
    }

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
    if (node == null) builder.append(UNKNOWN_PLACEHOLDER);
    else node.accept(this);
  }

  private void appendStrings(List<String> exprs, boolean withParen, String surround) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) builder.append("()");
      return;
    }

    if (withParen) builder.append('(');

    for (String arg : exprs) {
      if (arg == null) continue;
      if (surround != null) builder.append(surround);
      builder.append(arg);
      if (surround != null) builder.append(surround);
      builder.append(", ");
    }

    trimTrailing(builder, 2);

    if (withParen) builder.append(')');
  }

  private void appendNodes(List<SQLNode> exprs, boolean withParen, boolean noBreak) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) builder.append("()");
      return;
    }

    if (withParen) builder.append('(');

    for (int i = 0; i < exprs.size() - 1; i++) {
      final SQLNode expr = exprs.get(i);
      safeVisit(expr);
      builder.append(',');
      if (noBreak) builder.append(' ');
      else breakLine();
    }

    final SQLNode last = exprs.get(exprs.size() - 1);
    safeVisit(last);

    if (withParen) builder.append(')');
  }

  private void appendNodes(List<SQLNode> exprs, boolean withParen) {
    appendNodes(exprs, withParen, false);
  }

  private void appendNodes(List<SQLNode> exprs) {
    appendNodes(exprs, true);
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
