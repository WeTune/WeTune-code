package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.sqlparser.ast.constants.*;

import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.trimTrailing;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.ARRAY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.JOINED;

public class Formatter implements SQLVisitor {
  private static final String INDENT_STR = "  ";
  private static final String UNKNOWN_PLACEHOLDER = "<??>";

  protected final StringBuilder builder = new StringBuilder();
  private final boolean oneLine;

  private int indent = 0;

  public Formatter(boolean oneLine) {
    this.oneLine = oneLine;
  }

  private static char quotation(SQLNode node) {
    if (SQLNode.POSTGRESQL.equals(node.dbType())) {
      return '"';
    } else {
      return '`';
    }
  }

  private static String quotation2(SQLNode node) {
    if (SQLNode.POSTGRESQL.equals(node.dbType())) {
      return "\"";
    } else {
      return "`";
    }
  }

  protected Formatter append(Object o) {
    builder.append(o);
    return this;
  }

  protected Formatter append(int i) {
    builder.append(i);
    return this;
  }

  protected Formatter append(char c) {
    builder.append(c);
    return this;
  }

  protected Formatter append(String s) {
    builder.append(s);
    return this;
  }

  private void breakLine0() {
    append('\n');
  }

  private void increaseIndent() {
    if (!oneLine) ++indent;
  }

  private void decreaseIndent() {
    if (!oneLine) --indent;
  }

  private void insertIndent() {
    append(INDENT_STR.repeat(indent));
  }

  private void breakLine(boolean spaceIfOneLine) {
    if (!oneLine) {
      breakLine0();
      insertIndent();
    } else if (spaceIfOneLine) append(' ');
  }

  private void breakLine() {
    breakLine(true);
  }

  private void appendName(SQLNode node, String name, boolean withDot) {
    if (name == null) return;
    append(quotation(node)).append(name).append(quotation(node));
    if (withDot) append('.');
  }

  @Override
  public boolean enter(SQLNode node) {
    if (node.nodeType() == NodeType.INVALID) {
      append(UNKNOWN_PLACEHOLDER);
      return false;
    }
    return true;
  }

  @Override
  public boolean enterCreateTable(SQLNode createTable) {
    append("CREATE TABLE ");

    safeVisit(createTable.get(NodeAttr.CREATE_TABLE_NAME));

    append(" (");
    increaseIndent();

    for (var colDef : createTable.get(NodeAttr.CREATE_TABLE_COLUMNS)) {
      breakLine();
      safeVisit(colDef);
      append(',');
    }

    for (var conDef : createTable.get(NodeAttr.CREATE_TABLE_CONSTRAINTS)) {
      breakLine();
      safeVisit(conDef);
      append(',');
    }

    trimTrailing(builder, 1);
    decreaseIndent();
    breakLine();
    insertIndent();
    append(')');

    final String engine = createTable.get(NodeAttr.CREATE_TABLE_ENGINE);
    if (engine != null) append(" ENGINE = '").append(engine).append('\'');
    return false;
  }

  @Override
  public boolean enterTableName(SQLNode tableName) {
    appendName(tableName, tableName.get(NodeAttr.TABLE_NAME_SCHEMA), true);
    appendName(tableName, tableName.get(NodeAttr.TABLE_NAME_TABLE), false);

    return false;
  }

  @Override
  public boolean enterColumnDef(SQLNode colDef) {
    safeVisit(colDef.get(NodeAttr.COLUMN_DEF_NAME));
    append(' ').append(colDef.get(NodeAttr.COLUMN_DEF_DATATYPE_RAW));

    if (colDef.isFlag(NodeAttr.COLUMN_DEF_CONS, UNIQUE)) append(" UNIQUE");
    if (colDef.isFlag(NodeAttr.COLUMN_DEF_CONS, PRIMARY)) append(" PRIMARY KEY");
    if (colDef.isFlag(NodeAttr.COLUMN_DEF_CONS, NOT_NULL)) append(" NOT NULL");
    if (colDef.isFlag(NodeAttr.COLUMN_DEF_AUTOINCREMENT)) append(" AUTO_INCREMENT");

    final var references = colDef.get(NodeAttr.COLUMN_DEF_REF);
    if (references != null) safeVisit(references);

    return false;
  }

  @Override
  public boolean enterName2(SQLNode name2) {
    appendName(name2, name2.get(NodeAttr.NAME_2_0), true);
    appendName(name2, name2.get(NodeAttr.NAME_2_1), false);
    return false;
  }

  @Override
  public boolean enterName3(SQLNode name3) {
    appendName(name3, name3.get(NodeAttr.NAME_3_0), true);
    appendName(name3, name3.get(NodeAttr.NAME_3_1), true);
    appendName(name3, name3.get(NodeAttr.NAME_3_2), false);
    return false;
  }

  @Override
  public boolean enterColumnName(SQLNode colName) {
    appendName(colName, colName.get(NodeAttr.COLUMN_NAME_SCHEMA), true);
    appendName(colName, colName.get(NodeAttr.COLUMN_NAME_TABLE), true);
    appendName(colName, colName.get(NodeAttr.COLUMN_NAME_COLUMN), false);

    return false;
  }

  @Override
  public boolean enterCommonName(SQLNode commonName) {
    appendName(commonName, commonName.get(NodeAttr.NAME_3_0), true);
    appendName(commonName, commonName.get(NodeAttr.NAME_3_1), true);
    appendName(commonName, commonName.get(NodeAttr.NAME_3_2), false);
    return false;
  }

  @Override
  public boolean enterReferences(SQLNode ref) {
    append(" REFERENCES ");
    safeVisit(ref.get(NodeAttr.REFERENCES_TABLE));

    final var columns = ref.get(NodeAttr.REFERENCES_COLUMNS);
    if (columns != null) {
      try (final var ignored = withParen(true)) {
        for (SQLNode column : columns) {
          safeVisit(column);
          append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
      }
    }

    return false;
  }

  @Override
  public boolean enterIndexDef(SQLNode indexDef) {
    final var constraint = indexDef.get(NodeAttr.INDEX_DEF_CONS);
    final var type = indexDef.get(NodeAttr.INDEX_DEF_TYPE);
    final var name = indexDef.get(NodeAttr.INDEX_DEF_NAME);
    final var keys = indexDef.get(NodeAttr.INDEX_DEF_KEYS);
    final var refs = indexDef.get(NodeAttr.INDEX_DEF_REFS);

    if (constraint != null)
      switch (constraint) {
        case PRIMARY:
          append("PRIMARY ");
          break;
        case UNIQUE:
          append("UNIQUE ");
          break;
        case FOREIGN:
          append("FOREIGN ");
          break;
      }

    if (type != null)
      switch (type) {
        case FULLTEXT:
          append("FULLTEXT ");
          break;
        case SPATIAL:
          append("SPATIAL ");
          break;
      }

    append("KEY ");

    if (name != null) appendName(indexDef, name, false);

    try (final var ignored = withParen(true)) {
      for (SQLNode key : keys) {
        safeVisit(key);
        append(", ");
      }
      builder.delete(builder.length() - 2, builder.length());
    }

    if (refs != null) safeVisit(refs);

    if (type != null)
      switch (type) {
        case BTREE:
          append(" USING BTREE ");
          break;
        case RTREE:
          append(" USING RTREE ");
          break;
        case HASH:
          append(" USING HASH ");
          break;
      }

    return false;
  }

  @Override
  public boolean enterKeyPart(SQLNode keyPart) {
    final String columnName = keyPart.get(NodeAttr.KEY_PART_COLUMN);
    final Integer length = keyPart.get(NodeAttr.KEY_PART_LEN);
    final KeyDirection direction = keyPart.get(NodeAttr.KEY_PART_DIRECTION);
    final SQLNode expr = keyPart.get(NodeAttr.KEY_PART_EXPR);

    if (columnName != null) appendName(keyPart, columnName, false);
    if (length != null) append('(').append(length).append(')');
    if (direction != null) append(' ').append(direction);
    if (expr != null)
      try (final var ignored = withParen(true)) {
        safeVisit(expr);
      }

    return false;
  }

  @Override
  public boolean enterVariable(SQLNode variable) {
    append(variable.get(VARIABLE_SCOPE).prefix());
    append(variable.get(VARIABLE_NAME));

    final SQLNode assignment = variable.get(VARIABLE_ASSIGNMENT);
    if (assignment != null) {
      append(" = ");
      safeVisit(assignment);
    }

    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    final Object value = literal.get(LITERAL_VALUE);
    switch (literal.get(LITERAL_TYPE)) {
      case TEXT:
        append('\'').append(value).append('\'');
        break;
      case INTEGER:
      case LONG:
      case FRACTIONAL:
      case HEX:
        append(value);
        break;
      case BOOL:
        append(value.toString().toUpperCase());
        break;
      case NULL:
        append("NULL");
        break;
      case UNKNOWN:
        append("UNKNOWN");
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
    final SQLNode name = funcCall.get(FUNC_CALL_NAME);
    final List<SQLNode> args = funcCall.getOr(FUNC_CALL_ARGS, emptyList());

    final String schemaName = name.get(NodeAttr.NAME_2_0);
    final String funcName = name.get(NodeAttr.NAME_2_1);

    if (schemaName == null && "extract".equalsIgnoreCase(funcName)) {
      append("EXTRACT");
      try (final var ignored = withParen(true)) {
        if (args.size() != 2) append(UNKNOWN_PLACEHOLDER);
        else {
          safeVisit(args.get(0));
          append(" FROM ");
          safeVisit(args.get(1));
        }
      }
      return false;
    }

    if (schemaName == null && "position".equalsIgnoreCase(funcName)) {
      append("POSITION");
      try (final var ignored = withParen(true)) {
        if (args.size() != 2) append(UNKNOWN_PLACEHOLDER);
        else {
          safeVisit(args.get(0));
          append(" IN ");
          safeVisit(args.get(1));
        }
      }
      return false;
    }

    if (schemaName == null && "trim".equalsIgnoreCase(funcName)) {
      append("TRIM");
      try (final var ignored = withParen(true)) {
        if (args.size() != 3) append(UNKNOWN_PLACEHOLDER);
        else {
          final SQLNode arg0 = args.get(0);
          final SQLNode arg1 = args.get(1);
          final SQLNode arg2 = args.get(2);

          if (arg0 != null) {
            safeVisit(arg0); // LEADING/TRAILING/BOTH
            append(' ');
          }
          if (arg1 != null) safeVisit(arg1);
          if (arg2 != null) {
            if (arg1 != null) append(' ');
            append("FROM ");
            safeVisit(arg2);
          }
        }
      }
      return false;
    }

    if (schemaName == null && "overlay".equalsIgnoreCase(funcName)) {
      append("OVERLAY");
      try (final var ignored = withParen(true)) {
        if (args.size() < 3) append(UNKNOWN_PLACEHOLDER);
        else {
          final SQLNode arg0 = args.get(0);
          final SQLNode arg1 = args.get(1);
          final SQLNode arg2 = args.get(2);
          final SQLNode arg3 = args.size() > 3 ? args.get(3) : null;
          safeVisit(arg0);
          append(" PLACING ");
          safeVisit(arg1);
          append(" FROM ");
          safeVisit(arg2);
          if (arg3 != null) {
            append(" FOR ");
            safeVisit(arg3);
          }
        }
      }
      return false;
    }

    if (schemaName == null && args.isEmpty() && SQLNode.POSTGRESQL.equals(funcCall.dbType())) {
      final String upperCase = funcName.toUpperCase();
      if (upperCase.startsWith("CURRENT")
          || "SESSION_USER".equals(upperCase)
          || "USER".equals(upperCase)
          || "LOCALTIME".equals(upperCase)
          || "LOCALTIMESTAMP".equals(upperCase)) {
        append(upperCase);
        return false;
      }
    }

    appendName(name, schemaName, true);
    // we choose not quote the function name for beauty and convention
    // in most case this doesn't cause problem
    append(funcName.toUpperCase());

    appendNodes(args, true, true);
    return false;
  }

  @Override
  public boolean enterCollation(SQLNode collation) {
    final SQLNode expr = collation.get(COLLATE_EXPR);
    try (final var ignored = withParen(needParen(collation, expr, true))) {
      safeVisit(expr);
    }
    append(" COLLATE ");
    if (SQLNode.POSTGRESQL.equals(collation.dbType())) {
      collation.get(COLLATE_COLLATION).accept(this);
    } else {
      append('\'').append(collation.get(COLLATE_COLLATION).get(SYMBOL_TEXT)).append('\'');
    }
    return false;
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    if (SQLNode.POSTGRESQL.equals(paramMarker.dbType())) {
      append('$').append(paramMarker.getOr(PARAM_MARKER_NUMBER, 1));
    } else {
      append('?');
    }
    return false;
  }

  @Override
  public boolean enterUnary(SQLNode unary) {
    final String op = unary.get(UNARY_OP).text();
    append(op);
    if (op.length() > 1) append(' ');

    final SQLNode expr = unary.get(UNARY_EXPR);
    try (final var ignored = withParen(needParen(unary, expr, false))) {
      safeVisit(expr);
    }

    return false;
  }

  @Override
  public boolean enterGroupingOp(SQLNode groupingOp) {
    append("GROUPING");
    appendNodes(groupingOp.getOr(GROUPING_OP_EXPRS, emptyList()));
    return false;
  }

  @Override
  public boolean enterTuple(SQLNode tuple) {
    if (tuple.isFlag(TUPLE_AS_ROW)) append("ROW");
    appendNodes(tuple.getOr(TUPLE_EXPRS, emptyList()), true, true);
    return false;
  }

  @Override
  public boolean enterMatch(SQLNode match) {
    append("MATCH ");
    appendNodes(match.get(MATCH_COLS), false);

    append(" AGAINST (");
    safeVisit(match.get(MATCH_EXPR));

    final MatchOption option = match.get(MATCH_OPTION);
    if (option != null) append(' ').append(option.optionText());
    append(')');

    return false;
  }

  @Override
  public boolean enterCast(SQLNode cast) {
    if (SQLNode.POSTGRESQL.equals(cast.dbType())) {
      safeVisit(cast.get(CAST_EXPR));
      append("::");
      cast.get(CAST_TYPE).formatAsCastType(builder, SQLNode.POSTGRESQL);

    } else {
      append("CAST(");

      safeVisit(cast.get(CAST_EXPR));

      append(" AS ");
      final SQLDataType castType = cast.get(CAST_TYPE);
      castType.formatAsCastType(builder, cast.dbType());

      if (SQLNode.MYSQL.equals(cast.dbType())) if (cast.isFlag(CAST_IS_ARRAY)) append(" ARRAY");
      append(')');
    }
    return false;
  }

  @Override
  public boolean enterDefault(SQLNode _default) {
    append("DEFAULT(");
    safeVisit(_default.get(DEFAULT_COL));
    append(')');
    return false;
  }

  @Override
  public boolean enterValues(SQLNode values) {
    append("VALUES(");
    safeVisit(values.get(VALUES_EXPR));
    append(')');
    return false;
  }

  @Override
  public boolean enterSymbol(SQLNode symbol) {
    append(symbol.get(SYMBOL_TEXT).toUpperCase());
    return false;
  }

  @Override
  public boolean enterInterval(SQLNode interval) {
    append("INTERVAL ");
    final SQLNode expr = interval.get(INTERVAL_EXPR);
    try (final var ignored = withParen(needParen(interval, expr, false))) {
      safeVisit(expr);
    }
    append(' ').append(interval.get(INTERVAL_UNIT));

    return false;
  }

  @Override
  public boolean enterWildcard(SQLNode wildcard) {
    final SQLNode table = wildcard.get(WILDCARD_TABLE);
    if (table != null) {
      safeVisit(table);
      append('.');
    }
    append('*');
    return false;
  }

  @Override
  public boolean enterAggregate(SQLNode aggregate) {
    append(aggregate.get(AGGREGATE_NAME).toUpperCase()).append('(');
    if (aggregate.isFlag(AGGREGATE_DISTINCT)) append("DISTINCT ");

    appendNodes(aggregate.get(AGGREGATE_ARGS), false, true);
    final List<SQLNode> order = aggregate.get(AGGREGATE_ORDER);

    if (order != null && !order.isEmpty()) {
      append(" ORDER BY ");
      appendNodes(order, false);
    }

    final String sep = aggregate.get(AGGREGATE_SEP);
    if (sep != null) append(" SEPARATOR '").append(sep).append('\'');

    append(')');

    final String windowName = aggregate.get(AGGREGATE_WINDOW_NAME);
    final SQLNode windowSpec = aggregate.get(AGGREGATE_WINDOW_SPEC);
    if (windowName != null) {
      append(" OVER ");
      appendName(aggregate, windowName, false);
    }
    if (windowSpec != null) {
      append(" OVER ");
      safeVisit(windowSpec);
    }
    return false;
  }

  @Override
  public boolean enterExists(SQLNode exists) {
    append("EXISTS ");
    safeVisit(exists.get(EXISTS_SUBQUERY_EXPR));
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

    append(joinType.text()).append(' ');
    final boolean needParen =
        JOINED.isInstance(right) && !(joinType.isInner() && right.get(JOINED_TYPE).isInner());
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
          append("ON ");
          safeVisit(on);
        } else {
          append("USING ");
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
    append("CONVERT(");
    safeVisit(convertUsing.get(CONVERT_USING_EXPR));
    append(" USING ");

    String charset = convertUsing.get(CONVERT_USING_CHARSET).get(SYMBOL_TEXT);
    if (!charset.equalsIgnoreCase("binary") && !charset.equalsIgnoreCase("default"))
      charset = "'" + charset + "'";

    append(charset);
    append(')');
    return false;
  }

  @Override
  public boolean enterCase(SQLNode _case) {
    append("CASE");
    final SQLNode cond = _case.get(CASE_COND);
    if (cond != null) {
      append(' ');
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
      append("ELSE ");
      try (final var ignored = withParen(needParen(_case, _else, false))) {
        safeVisit(_else);
      }
    }

    decreaseIndent();
    breakLine();
    append("END");

    return false;
  }

  @Override
  public boolean enterWhen(SQLNode when) {
    append("WHEN ");
    final SQLNode cond = when.get(WHEN_COND);
    try (final var ignored = withParen(needParen(when, cond, false))) {
      safeVisit(cond);
    }

    append(" THEN ");
    final SQLNode expr = when.get(WHEN_EXPR);
    try (final var ignored = withParen(needParen(when, expr, false))) {
      safeVisit(expr);
    }
    return false;
  }

  @Override
  public boolean enterWindowFrame(SQLNode windowFrame) {
    append(windowFrame.get(NodeAttr.WINDOW_FRAME_UNIT)).append(' ');
    final SQLNode start = windowFrame.get(NodeAttr.WINDOW_FRAME_START);
    final SQLNode end = windowFrame.get(NodeAttr.WINDOW_FRAME_END);

    if (end != null) append("BETWEEN ");
    safeVisit(start);
    if (end != null) {
      append(" AND ");
      safeVisit(end);
    }

    final WindowExclusion exclusion = windowFrame.get(NodeAttr.WINDOW_FRAME_EXCLUSION);
    if (exclusion != null) append(" EXCLUDE ").append(exclusion.text());

    return false;
  }

  @Override
  public boolean enterFrameBound(SQLNode frameBound) {
    if (frameBound.get(NodeAttr.FRAME_BOUND_DIRECTION) == null) append("CURRENT ROW");
    else {
      safeVisit(frameBound.get(NodeAttr.FRAME_BOUND_EXPR));
      append(' ').append(frameBound.get(NodeAttr.FRAME_BOUND_DIRECTION));
    }

    return false;
  }

  @Override
  public boolean enterWindowSpec(SQLNode windowSpec) {
    final String alias = windowSpec.get(NodeAttr.WINDOW_SPEC_ALIAS);
    if (alias != null) {
      appendName(windowSpec, alias, false);
      append(" AS ");
    }

    append("(");

    final String name = windowSpec.get(NodeAttr.WINDOW_SPEC_NAME);
    if (name != null) appendName(windowSpec, name, false);

    final List<SQLNode> partition = windowSpec.get(NodeAttr.WINDOW_SPEC_PARTITION);
    if (partition != null && !partition.isEmpty()) {
      append(" PARTITION BY ");
      appendNodes(partition, false);
    }

    final List<SQLNode> order = windowSpec.get(NodeAttr.WINDOW_SPEC_ORDER);
    if (order != null && !order.isEmpty()) {
      append(" ORDER BY ");
      appendNodes(order, false);
    }

    final SQLNode frame = windowSpec.get(NodeAttr.WINDOW_SPEC_FRAME);
    if (frame != null) {
      append(' ');
      safeVisit(frame);
    }

    append(')');

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
    else append(' ');

    append(op.text()).append(' ');

    final SubqueryOption subqueryOption = binary.get(BINARY_SUBQUERY_OPTION);
    if (subqueryOption != null) append(subqueryOption).append(' ');

    final SQLNode right = binary.get(BINARY_RIGHT);

    final boolean needParen =
        op == BinaryOp.MEMBER_OF || ARRAY.isInstance(right) || needParen(binary, right, false);
    final boolean needIndent = needParen && op.isLogic();

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

    append(' ').append(operator.text0()).append(' ');
    try (final var ignored = withParen(needParen(ternary, middle, false))) {
      safeVisit(middle);
    }

    append(' ').append(operator.text1()).append(' ');
    try (final var ignored = withParen(needParen(ternary, right, false))) {
      safeVisit(right);
    }

    return false;
  }

  @Override
  public boolean enterOrderItem(SQLNode orderItem) {
    safeVisit(orderItem.get(NodeAttr.ORDER_ITEM_EXPR));
    final KeyDirection direction = orderItem.get(NodeAttr.ORDER_ITEM_DIRECTION);
    if (direction != null) append(' ').append(direction.name());
    return false;
  }

  @Override
  public boolean enterSelectItem(SQLNode selectItem) {
    safeVisit(selectItem.get(NodeAttr.SELECT_ITEM_EXPR));

    final String alias = selectItem.get(NodeAttr.SELECT_ITEM_ALIAS);
    if (alias != null) {
      append(" AS ");
      appendName(selectItem, alias, false);
    }
    return false;
  }

  @Override
  public boolean enterIndexHint(SQLNode indexHint) {
    final IndexHintType type = indexHint.get(NodeAttr.INDEX_HINT_TYPE);
    append(type).append(" INDEX");

    final IndexHintTarget target = indexHint.get(NodeAttr.INDEX_HINT_TARGET);
    if (target != null) append(" FOR ").append(target.text());

    append(' ');
    try (final var ignored = withParen(true)) {
      final List<String> names = indexHint.get(NodeAttr.INDEX_HINT_NAMES);

      if (names != null && !names.isEmpty()) {
        for (String name : names) {
          if (name.equalsIgnoreCase("primary")) append("PRIMARY");
          else appendName(indexHint, name, false);
          append(", ");
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
      append(" PARTITION ");
      appendStrings(partitions, true, quotation2(simpleTableSource));
    }

    final String alias = simpleTableSource.get(SIMPLE_ALIAS);
    if (alias != null) {
      append(" AS ");
      appendName(simpleTableSource, alias, false);
    }

    final List<SQLNode> hints = simpleTableSource.get(SIMPLE_HINTS);
    if (SQLNode.MYSQL.equals(simpleTableSource.dbType()) && hints != null && !hints.isEmpty()) {
      append(' ');
      appendNodes(hints, false);
    }

    return false;
  }

  @Override
  public boolean enterQuery(SQLNode query) {
    safeVisit(query.get(NodeAttr.QUERY_BODY));

    final List<SQLNode> orderBy = query.get(NodeAttr.QUERY_ORDER_BY);
    if (orderBy != null) {
      breakLine();
      append("ORDER BY");
      increaseIndent();
      breakLine();
      appendNodes(orderBy, false);
      decreaseIndent();
    }

    final SQLNode offset = query.get(NodeAttr.QUERY_OFFSET);
    final SQLNode limit = query.get(NodeAttr.QUERY_LIMIT);

    if (limit != null) {
      breakLine();
      append("LIMIT ");
      safeVisit(limit);
      if (offset != null) {
        append(" OFFSET ");
        safeVisit(offset);
      }
    }

    return false;
  }

  @Override
  public boolean enterQuerySpec(SQLNode querySpec) {
    final boolean distinct = querySpec.isFlag(NodeAttr.QUERY_SPEC_DISTINCT);
    final List<SQLNode> selectItems = querySpec.get(NodeAttr.QUERY_SPEC_SELECT_ITEMS);
    final SQLNode from = querySpec.get(NodeAttr.QUERY_SPEC_FROM);
    final SQLNode where = querySpec.get(NodeAttr.QUERY_SPEC_WHERE);
    final List<SQLNode> groupBy = querySpec.get(NodeAttr.QUERY_SPEC_GROUP_BY);
    final OLAPOption olapOption = querySpec.get(NodeAttr.QUERY_SPEC_OLAP_OPTION);
    final SQLNode having = querySpec.get(NodeAttr.QUERY_SPEC_HAVING);
    final List<SQLNode> windows = querySpec.get(NodeAttr.QUERY_SPEC_WINDOWS);

    append("SELECT");
    if (distinct) append(" DISTINCT");
    if (distinct && SQLNode.POSTGRESQL.equals(querySpec.dbType())) {
      final List<SQLNode> distinctOn = querySpec.get(NodeAttr.QUERY_SPEC_DISTINCT_ON);
      if (distinctOn != null && !distinctOn.isEmpty()) {
        append(" ON");
        appendNodes(distinctOn, true, true);
      }
    }

    increaseIndent();
    breakLine();
    appendNodes(selectItems, false);
    decreaseIndent();

    if (from != null) {
      breakLine();
      append("FROM ");
      increaseIndent();
      safeVisit(from);
      decreaseIndent();
    }

    if (where != null) {
      breakLine();
      append("WHERE");
      increaseIndent();
      breakLine();
      safeVisit(where);
      decreaseIndent();
    }

    if (groupBy != null) {
      breakLine();
      append("GROUP BY");
      increaseIndent();
      breakLine();
      appendNodes(groupBy, false);
      if (olapOption != null) {
        breakLine();
        append(olapOption.text());
      }
      decreaseIndent();
    }

    if (having != null) {
      breakLine();
      append("HAVING");
      increaseIndent();
      breakLine();
      safeVisit(having);
      decreaseIndent();
    }

    if (windows != null) {
      breakLine();
      append("WINDOW");
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
      increaseIndent();
      breakLine(false);
      safeVisit(queryExpr.get(QUERY_EXPR_QUERY));
      decreaseIndent();
      breakLine(false);
    }
    return false;
  }

  @Override
  public boolean enterSetOp(SQLNode setOp) {
    try (final var ignored = withParen(true)) {
      safeVisit(setOp.get(NodeAttr.SET_OP_LEFT));
    }

    breakLine();
    append(setOp.get(NodeAttr.SET_OP_TYPE));

    final SetOperationOption option = setOp.get(NodeAttr.SET_OP_OPTION);
    if (option != null) append(' ').append(option);
    breakLine();

    try (final var ignored = withParen(true)) {
      safeVisit(setOp.get(NodeAttr.SET_OP_RIGHT));
    }

    return false;
  }

  @Override
  public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    if (derivedTableSource.isFlag(DERIVED_LATERAL)) append("LATERAL ");
    try (final var ignored = withParen(true)) {
      increaseIndent();
      breakLine(false);
      safeVisit(derivedTableSource.get(DERIVED_SUBQUERY));
      decreaseIndent();
      breakLine(false);
    }

    final String alias = derivedTableSource.get(DERIVED_ALIAS);
    if (alias != null) {
      append(" AS ");
      appendName(derivedTableSource, alias, false);
    }

    final List<String> internalRefs = derivedTableSource.get(DERIVED_INTERNAL_REFS);
    if (internalRefs != null && !internalRefs.isEmpty()) {
      breakLine();
      appendStrings(internalRefs, true, quotation2(derivedTableSource));
    }

    return false;
  }

  @Override
  public boolean enterArray(SQLNode array) {
    append("ARRAY[");
    appendNodes(array.get(ARRAY_ELEMENTS), false, true);
    append(']');
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
      append('(');
    }

    @Override
    public void close() {
      append(')');
    }
  }

  private void safeVisit(SQLNode node) {
    if (node == null) append(UNKNOWN_PLACEHOLDER);
    else node.accept(this);
  }

  private void appendStrings(List<String> exprs, boolean withParen, String surround) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) append("()");
      return;
    }

    if (withParen) append('(');

    for (String arg : exprs) {
      if (arg == null) continue;
      if (surround != null) append(surround);
      append(arg);
      if (surround != null) append(surround);
      append(", ");
    }

    trimTrailing(builder, 2);

    if (withParen) append(')');
  }

  private void appendNodes(List<SQLNode> exprs, boolean withParen, boolean noBreak) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) append("()");
      return;
    }

    if (withParen) append('(');

    for (int i = 0; i < exprs.size() - 1; i++) {
      final SQLNode expr = exprs.get(i);
      safeVisit(expr);
      append(',');
      if (noBreak) append(' ');
      else breakLine();
    }

    final SQLNode last = exprs.get(exprs.size() - 1);
    safeVisit(last);

    if (withParen) append(')');
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
