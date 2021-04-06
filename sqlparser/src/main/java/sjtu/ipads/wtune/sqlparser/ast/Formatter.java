package sjtu.ipads.wtune.sqlparser.ast;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.trimTrailing;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_ARGS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_ORDER;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_SEP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_WINDOW_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_WINDOW_SPEC;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.ARRAY_ELEMENTS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_SUBQUERY_OPTION;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CASE_COND;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CASE_ELSE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CASE_WHENS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CAST_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CAST_IS_ARRAY;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CAST_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLLATE_COLLATION;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLLATE_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CONVERT_USING_CHARSET;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CONVERT_USING_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.DEFAULT_COL;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.EXISTS_SUBQUERY_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_ARGS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.GROUPING_OP_EXPRS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.INTERVAL_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.INTERVAL_UNIT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_UNIT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.MATCH_COLS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.MATCH_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.MATCH_OPTION;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.PARAM_MARKER_NUMBER;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.QUERY_EXPR_QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.SYMBOL_TEXT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TERNARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TERNARY_MIDDLE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TERNARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TERNARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TUPLE_AS_ROW;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TUPLE_EXPRS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.UNARY_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.UNARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.VALUES_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.VARIABLE_ASSIGNMENT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.VARIABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.VARIABLE_SCOPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WHEN_COND;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WHEN_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.getOperatorPrecedence;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_DEF_AUTOINCREMENT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_DEF_CONS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_DEF_DATATYPE_RAW;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_DEF_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_DEF_REF;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_SCHEMA;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.CREATE_TABLE_COLUMNS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.CREATE_TABLE_CONSTRAINTS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.CREATE_TABLE_ENGINE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.CREATE_TABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.FRAME_BOUND_DIRECTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.FRAME_BOUND_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_DEF_CONS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_DEF_KEYS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_DEF_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_DEF_REFS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_DEF_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_HINT_NAMES;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_HINT_TARGET;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.INDEX_HINT_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.KEY_PART_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.KEY_PART_DIRECTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.KEY_PART_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.KEY_PART_LEN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_2_0;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_2_1;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_3_0;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_3_1;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_3_2;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.ORDER_ITEM_DIRECTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.ORDER_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_LIMIT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_OFFSET;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_ORDER_BY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_DISTINCT_ON;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_GROUP_BY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_OLAP_OPTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_SELECT_ITEMS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WINDOWS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.REFERENCES_COLUMNS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.REFERENCES_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_OPTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_SCHEMA;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_FRAME_END;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_FRAME_EXCLUSION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_FRAME_START;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_FRAME_UNIT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_SPEC_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_SPEC_FRAME;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_SPEC_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_SPEC_ORDER;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.WINDOW_SPEC_PARTITION;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_INTERNAL_REFS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_LATERAL;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_ON;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_USING;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_HINTS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_PARTITIONS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.NOT_NULL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.PRIMARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.UNIQUE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.ARRAY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.IndexHintTarget;
import sjtu.ipads.wtune.sqlparser.ast.constants.IndexHintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;
import sjtu.ipads.wtune.sqlparser.ast.constants.MatchOption;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.ast.constants.OLAPOption;
import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperationOption;
import sjtu.ipads.wtune.sqlparser.ast.constants.SubqueryOption;
import sjtu.ipads.wtune.sqlparser.ast.constants.TernaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.WindowExclusion;

public class Formatter implements ASTVistor {
  private static final String INDENT_STR = "  ";
  private static final String UNKNOWN_PLACEHOLDER = "<??>";

  protected final StringBuilder builder = new StringBuilder();
  private final boolean oneLine;

  private int indent = 0;

  public Formatter(boolean oneLine) {
    this.oneLine = oneLine;
  }

  private static char quotation(ASTNode node) {
    if (ASTNode.POSTGRESQL.equals(node.dbType())) {
      return '"';
    } else {
      return '`';
    }
  }

  private static String quotation2(ASTNode node) {
    if (ASTNode.POSTGRESQL.equals(node.dbType())) {
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

  private void appendName(ASTNode node, String name, boolean withDot) {
    if (name == null) return;
    append(quotation(node)).append(name).append(quotation(node));
    if (withDot) append('.');
  }

  @Override
  public boolean enter(ASTNode node) {
    if (node.nodeType() == NodeType.INVALID) {
      append(UNKNOWN_PLACEHOLDER);
      return false;
    }
    return true;
  }

  @Override
  public boolean enterCreateTable(ASTNode createTable) {
    append("CREATE TABLE ");

    safeVisit(createTable.get(CREATE_TABLE_NAME));

    append(" (");
    increaseIndent();

    for (var colDef : createTable.get(CREATE_TABLE_COLUMNS)) {
      breakLine();
      safeVisit(colDef);
      append(',');
    }

    for (var conDef : createTable.get(CREATE_TABLE_CONSTRAINTS)) {
      breakLine();
      safeVisit(conDef);
      append(',');
    }

    trimTrailing(builder, 1);
    decreaseIndent();
    breakLine();
    insertIndent();
    append(')');

    final String engine = createTable.get(CREATE_TABLE_ENGINE);
    if (engine != null) append(" ENGINE = '").append(engine).append('\'');
    return false;
  }

  @Override
  public boolean enterTableName(ASTNode tableName) {
    appendName(tableName, tableName.get(TABLE_NAME_SCHEMA), true);
    appendName(tableName, tableName.get(TABLE_NAME_TABLE), false);

    return false;
  }

  @Override
  public boolean enterColumnDef(ASTNode colDef) {
    safeVisit(colDef.get(COLUMN_DEF_NAME));
    append(' ').append(colDef.get(COLUMN_DEF_DATATYPE_RAW));

    if (colDef.isFlag(COLUMN_DEF_CONS, UNIQUE)) append(" UNIQUE");
    if (colDef.isFlag(COLUMN_DEF_CONS, PRIMARY)) append(" PRIMARY KEY");
    if (colDef.isFlag(COLUMN_DEF_CONS, NOT_NULL)) append(" NOT NULL");
    if (colDef.isFlag(COLUMN_DEF_AUTOINCREMENT)) append(" AUTO_INCREMENT");

    final var references = colDef.get(COLUMN_DEF_REF);
    if (references != null) safeVisit(references);

    return false;
  }

  @Override
  public boolean enterName2(ASTNode name2) {
    appendName(name2, name2.get(NAME_2_0), true);
    appendName(name2, name2.get(NAME_2_1), false);
    return false;
  }

  @Override
  public boolean enterName3(ASTNode name3) {
    appendName(name3, name3.get(NAME_3_0), true);
    appendName(name3, name3.get(NAME_3_1), true);
    appendName(name3, name3.get(NAME_3_2), false);
    return false;
  }

  @Override
  public boolean enterColumnName(ASTNode colName) {
    appendName(colName, colName.get(COLUMN_NAME_SCHEMA), true);
    appendName(colName, colName.get(COLUMN_NAME_TABLE), true);
    appendName(colName, colName.get(COLUMN_NAME_COLUMN), false);

    return false;
  }

  @Override
  public boolean enterCommonName(ASTNode commonName) {
    appendName(commonName, commonName.get(NAME_3_0), true);
    appendName(commonName, commonName.get(NAME_3_1), true);
    appendName(commonName, commonName.get(NAME_3_2), false);
    return false;
  }

  @Override
  public boolean enterReferences(ASTNode ref) {
    append(" REFERENCES ");
    safeVisit(ref.get(REFERENCES_TABLE));

    final var columns = ref.get(REFERENCES_COLUMNS);
    if (columns != null) {
      try (final var ignored = withParen(true)) {
        for (ASTNode column : columns) {
          safeVisit(column);
          append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
      }
    }

    return false;
  }

  @Override
  public boolean enterIndexDef(ASTNode indexDef) {
    final var constraint = indexDef.get(INDEX_DEF_CONS);
    final var type = indexDef.get(INDEX_DEF_TYPE);
    final var name = indexDef.get(INDEX_DEF_NAME);
    final var keys = indexDef.get(INDEX_DEF_KEYS);
    final var refs = indexDef.get(INDEX_DEF_REFS);

    if (constraint != null)
      switch (constraint) {
        case PRIMARY -> append("PRIMARY ");
        case UNIQUE -> append("UNIQUE ");
        case FOREIGN -> append("FOREIGN ");
      }

    if (type != null)
      switch (type) {
        case FULLTEXT -> append("FULLTEXT ");
        case SPATIAL -> append("SPATIAL ");
      }

    append("KEY ");

    if (name != null) appendName(indexDef, name, false);

    try (final var ignored = withParen(true)) {
      for (ASTNode key : keys) {
        safeVisit(key);
        append(", ");
      }
      builder.delete(builder.length() - 2, builder.length());
    }

    if (refs != null) safeVisit(refs);

    if (type != null)
      switch (type) {
        case BTREE -> append(" USING BTREE ");
        case RTREE -> append(" USING RTREE ");
        case HASH -> append(" USING HASH ");
      }

    return false;
  }

  @Override
  public boolean enterKeyPart(ASTNode keyPart) {
    final String columnName = keyPart.get(KEY_PART_COLUMN);
    final Integer length = keyPart.get(KEY_PART_LEN);
    final KeyDirection direction = keyPart.get(KEY_PART_DIRECTION);
    final ASTNode expr = keyPart.get(KEY_PART_EXPR);

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
  public boolean enterVariable(ASTNode variable) {
    append(variable.get(VARIABLE_SCOPE).prefix());
    append(variable.get(VARIABLE_NAME));

    final ASTNode assignment = variable.get(VARIABLE_ASSIGNMENT);
    if (assignment != null) {
      append(" = ");
      safeVisit(assignment);
    }

    return false;
  }

  @Override
  public boolean enterLiteral(ASTNode literal) {
    final Object value = literal.get(LITERAL_VALUE);
    switch (literal.get(LITERAL_TYPE)) {
      case TEXT -> append('\'').append(value).append('\'');
      case INTEGER, LONG, FRACTIONAL, HEX -> append(value);
      case BOOL -> append(value.toString().toUpperCase());
      case NULL -> append("NULL");
      case UNKNOWN -> append("UNKNOWN");
      case TEMPORAL -> builder
          .append(literal.get(LITERAL_UNIT).toUpperCase())
          .append(" '")
          .append(value)
          .append('\'');
    }

    return false;
  }

  @Override
  public boolean enterFuncCall(ASTNode funcCall) {
    final ASTNode name = funcCall.get(FUNC_CALL_NAME);
    final List<ASTNode> args = funcCall.getOr(FUNC_CALL_ARGS, emptyList());

    final String schemaName = name.get(NAME_2_0);
    final String funcName = name.get(NAME_2_1);

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
          final ASTNode arg0 = args.get(0);
          final ASTNode arg1 = args.get(1);
          final ASTNode arg2 = args.get(2);

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
          final ASTNode arg0 = args.get(0);
          final ASTNode arg1 = args.get(1);
          final ASTNode arg2 = args.get(2);
          final ASTNode arg3 = args.size() > 3 ? args.get(3) : null;
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

    if (schemaName == null && args.isEmpty() && ASTNode.POSTGRESQL.equals(funcCall.dbType())) {
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
  public boolean enterCollation(ASTNode collation) {
    final ASTNode expr = collation.get(COLLATE_EXPR);
    try (final var ignored = withParen(needParen(collation, expr, true))) {
      safeVisit(expr);
    }
    append(" COLLATE ");
    if (ASTNode.POSTGRESQL.equals(collation.dbType())) {
      collation.get(COLLATE_COLLATION).accept(this);
    } else {
      append('\'').append(collation.get(COLLATE_COLLATION).get(SYMBOL_TEXT)).append('\'');
    }
    return false;
  }

  @Override
  public boolean enterParamMarker(ASTNode paramMarker) {
    if (ASTNode.POSTGRESQL.equals(paramMarker.dbType())) {
      append('$').append(paramMarker.getOr(PARAM_MARKER_NUMBER, 1));
    } else {
      append('?');
    }
    return false;
  }

  @Override
  public boolean enterUnary(ASTNode unary) {
    final String op = unary.get(UNARY_OP).text();
    append(op);
    if (op.length() > 1) append(' ');

    final ASTNode expr = unary.get(UNARY_EXPR);
    try (final var ignored = withParen(needParen(unary, expr, false))) {
      safeVisit(expr);
    }

    return false;
  }

  @Override
  public boolean enterGroupingOp(ASTNode groupingOp) {
    append("GROUPING");
    appendNodes(groupingOp.getOr(GROUPING_OP_EXPRS, emptyList()));
    return false;
  }

  @Override
  public boolean enterTuple(ASTNode tuple) {
    if (tuple.isFlag(TUPLE_AS_ROW)) append("ROW");
    appendNodes(tuple.getOr(TUPLE_EXPRS, emptyList()), true, true);
    return false;
  }

  @Override
  public boolean enterMatch(ASTNode match) {
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
  public boolean enterCast(ASTNode cast) {
    if (ASTNode.POSTGRESQL.equals(cast.dbType())) {
      safeVisit(cast.get(CAST_EXPR));
      append("::");
      cast.get(CAST_TYPE).formatAsCastType(builder, ASTNode.POSTGRESQL);

    } else {
      append("CAST(");

      safeVisit(cast.get(CAST_EXPR));

      append(" AS ");
      final SQLDataType castType = cast.get(CAST_TYPE);
      castType.formatAsCastType(builder, cast.dbType());

      if ((MYSQL.equals(cast.dbType()) || cast.dbType() == null)
           && cast.isFlag(CAST_IS_ARRAY))
        append(" ARRAY");
      append(')');
    }
    return false;
  }

  @Override
  public boolean enterDefault(ASTNode _default) {
    append("DEFAULT(");
    safeVisit(_default.get(DEFAULT_COL));
    append(')');
    return false;
  }

  @Override
  public boolean enterValues(ASTNode values) {
    append("VALUES(");
    safeVisit(values.get(VALUES_EXPR));
    append(')');
    return false;
  }

  @Override
  public boolean enterSymbol(ASTNode symbol) {
    append(symbol.get(SYMBOL_TEXT).toUpperCase());
    return false;
  }

  @Override
  public boolean enterInterval(ASTNode interval) {
    append("INTERVAL ");
    final ASTNode expr = interval.get(INTERVAL_EXPR);
    try (final var ignored = withParen(needParen(interval, expr, false))) {
      safeVisit(expr);
    }
    append(' ').append(interval.get(INTERVAL_UNIT));

    return false;
  }

  @Override
  public boolean enterWildcard(ASTNode wildcard) {
    final ASTNode table = wildcard.get(WILDCARD_TABLE);
    if (table != null) {
      safeVisit(table);
      append('.');
    }
    append('*');
    return false;
  }

  @Override
  public boolean enterAggregate(ASTNode aggregate) {
    append(aggregate.get(AGGREGATE_NAME).toUpperCase()).append('(');
    if (aggregate.isFlag(AGGREGATE_DISTINCT)) append("DISTINCT ");

    appendNodes(aggregate.get(AGGREGATE_ARGS), false, true);
    final List<ASTNode> order = aggregate.get(AGGREGATE_ORDER);

    if (order != null && !order.isEmpty()) {
      append(" ORDER BY ");
      appendNodes(order, false);
    }

    final String sep = aggregate.get(AGGREGATE_SEP);
    if (sep != null) append(" SEPARATOR '").append(sep).append('\'');

    append(')');

    final String windowName = aggregate.get(AGGREGATE_WINDOW_NAME);
    final ASTNode windowSpec = aggregate.get(AGGREGATE_WINDOW_SPEC);
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
  public boolean enterExists(ASTNode exists) {
    append("EXISTS ");
    safeVisit(exists.get(EXISTS_SUBQUERY_EXPR));
    return false;
  }

  @Override
  public boolean enterJoinedTableSource(ASTNode joinedTableSource) {
    final ASTNode left = joinedTableSource.get(JOINED_LEFT);
    final ASTNode right = joinedTableSource.get(JOINED_RIGHT);
    final JoinType joinType = joinedTableSource.get(JOINED_TYPE);
    final ASTNode on = joinedTableSource.get(JOINED_ON);
    final List<String> using = joinedTableSource.get(JOINED_USING);

    safeVisit(left);

    breakLine();

    append(joinType.text()).append(' ');
    final boolean needParen = JOINED_SOURCE.isInstance(right);
    try (final var ignored = withParen(needParen)) {
      if (needParen) {
        increaseIndent();
        breakLine(false);
      }

      safeVisit(right);

      if (needParen) {
        decreaseIndent();
        breakLine(false);
      }
    }

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

    return false;
  }

  @Override
  public boolean enterConvertUsing(ASTNode convertUsing) {
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
  public boolean enterCase(ASTNode _case) {
    append("CASE");
    final ASTNode cond = _case.get(CASE_COND);
    if (cond != null) {
      append(' ');
      try (final var ignored = withParen(needParen(_case, cond, false))) {
        safeVisit(cond);
      }
    }

    increaseIndent();
    for (ASTNode when : _case.get(CASE_WHENS)) {
      breakLine();
      safeVisit(when);
    }

    final ASTNode _else = _case.get(CASE_ELSE);
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
  public boolean enterWhen(ASTNode when) {
    append("WHEN ");
    final ASTNode cond = when.get(WHEN_COND);
    try (final var ignored = withParen(needParen(when, cond, false))) {
      safeVisit(cond);
    }

    append(" THEN ");
    final ASTNode expr = when.get(WHEN_EXPR);
    try (final var ignored = withParen(needParen(when, expr, false))) {
      safeVisit(expr);
    }
    return false;
  }

  @Override
  public boolean enterWindowFrame(ASTNode windowFrame) {
    append(windowFrame.get(WINDOW_FRAME_UNIT)).append(' ');
    final ASTNode start = windowFrame.get(WINDOW_FRAME_START);
    final ASTNode end = windowFrame.get(WINDOW_FRAME_END);

    if (end != null) append("BETWEEN ");
    safeVisit(start);
    if (end != null) {
      append(" AND ");
      safeVisit(end);
    }

    final WindowExclusion exclusion = windowFrame.get(WINDOW_FRAME_EXCLUSION);
    if (exclusion != null) append(" EXCLUDE ").append(exclusion.text());

    return false;
  }

  @Override
  public boolean enterFrameBound(ASTNode frameBound) {
    if (frameBound.get(FRAME_BOUND_DIRECTION) == null) append("CURRENT ROW");
    else {
      safeVisit(frameBound.get(FRAME_BOUND_EXPR));
      append(' ').append(frameBound.get(FRAME_BOUND_DIRECTION));
    }

    return false;
  }

  @Override
  public boolean enterWindowSpec(ASTNode windowSpec) {
    final String alias = windowSpec.get(WINDOW_SPEC_ALIAS);
    if (alias != null) {
      appendName(windowSpec, alias, false);
      append(" AS ");
    }

    append("(");

    final String name = windowSpec.get(WINDOW_SPEC_NAME);
    if (name != null) appendName(windowSpec, name, false);

    final List<ASTNode> partition = windowSpec.get(WINDOW_SPEC_PARTITION);
    if (partition != null && !partition.isEmpty()) {
      append(" PARTITION BY ");
      appendNodes(partition, false);
    }

    final List<ASTNode> order = windowSpec.get(WINDOW_SPEC_ORDER);
    if (order != null && !order.isEmpty()) {
      append(" ORDER BY ");
      appendNodes(order, false);
    }

    final ASTNode frame = windowSpec.get(WINDOW_SPEC_FRAME);
    if (frame != null) {
      append(' ');
      safeVisit(frame);
    }

    append(')');

    return false;
  }

  @Override
  public boolean enterBinary(ASTNode binary) {
    final ASTNode left = binary.get(BINARY_LEFT);
    try (final var ignored = withParen(needParen(binary, left, true))) {
      safeVisit(left);
    }

    final BinaryOp op = binary.get(BINARY_OP);

    if (op.isLogic()) breakLine();
    else append(' ');

    append(op.text()).append(' ');

    final SubqueryOption subqueryOption = binary.get(BINARY_SUBQUERY_OPTION);
    if (subqueryOption != null) append(subqueryOption).append(' ');

    final ASTNode right = binary.get(BINARY_RIGHT);

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
  public boolean enterTernary(ASTNode ternary) {
    final TernaryOp operator = ternary.get(TERNARY_OP);
    final ASTNode left = ternary.get(TERNARY_LEFT);
    final ASTNode middle = ternary.get(TERNARY_MIDDLE);
    final ASTNode right = ternary.get(TERNARY_RIGHT);

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
  public boolean enterOrderItem(ASTNode orderItem) {
    safeVisit(orderItem.get(ORDER_ITEM_EXPR));
    final KeyDirection direction = orderItem.get(ORDER_ITEM_DIRECTION);
    if (direction != null) append(' ').append(direction.name());
    return false;
  }

  @Override
  public boolean enterSelectItem(ASTNode selectItem) {
    safeVisit(selectItem.get(SELECT_ITEM_EXPR));

    final String alias = selectItem.get(SELECT_ITEM_ALIAS);
    if (alias != null) {
      append(" AS ");
      appendName(selectItem, alias, false);
    }
    return false;
  }

  @Override
  public boolean enterIndexHint(ASTNode indexHint) {
    final IndexHintType type = indexHint.get(INDEX_HINT_TYPE);
    append(type).append(" INDEX");

    final IndexHintTarget target = indexHint.get(INDEX_HINT_TARGET);
    if (target != null) append(" FOR ").append(target.text());

    append(' ');
    try (final var ignored = withParen(true)) {
      final List<String> names = indexHint.get(INDEX_HINT_NAMES);

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
  public boolean enterSimpleTableSource(ASTNode simpleTableSource) {
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

    final List<ASTNode> hints = simpleTableSource.get(SIMPLE_HINTS);
    if ((simpleTableSource.dbType() == null || MYSQL.equals(simpleTableSource.dbType()))
         && hints != null && !hints.isEmpty()) {
      append(' ');
      appendNodes(hints, false);
    }

    return false;
  }

  @Override
  public boolean enterQuery(ASTNode query) {
    safeVisit(query.get(QUERY_BODY));

    final List<ASTNode> orderBy = query.get(QUERY_ORDER_BY);
    if (orderBy != null) {
      breakLine();
      append("ORDER BY");
      increaseIndent();
      breakLine();
      appendNodes(orderBy, false);
      decreaseIndent();
    }

    final ASTNode offset = query.get(QUERY_OFFSET);
    final ASTNode limit = query.get(QUERY_LIMIT);

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
  public boolean enterQuerySpec(ASTNode querySpec) {
    final boolean distinct = querySpec.isFlag(QUERY_SPEC_DISTINCT);
    final List<ASTNode> selectItems = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final ASTNode from = querySpec.get(QUERY_SPEC_FROM);
    final ASTNode where = querySpec.get(QUERY_SPEC_WHERE);
    final List<ASTNode> groupBy = querySpec.get(QUERY_SPEC_GROUP_BY);
    final OLAPOption olapOption = querySpec.get(QUERY_SPEC_OLAP_OPTION);
    final ASTNode having = querySpec.get(QUERY_SPEC_HAVING);
    final List<ASTNode> windows = querySpec.get(QUERY_SPEC_WINDOWS);

    append("SELECT");
    if (distinct) append(" DISTINCT");
    if (distinct && ASTNode.POSTGRESQL.equals(querySpec.dbType())) {
      final List<ASTNode> distinctOn = querySpec.get(QUERY_SPEC_DISTINCT_ON);
      if (distinctOn != null && !distinctOn.isEmpty()) {
        append(" ON ");
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
  public boolean enterQueryExpr(ASTNode queryExpr) {
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
  public boolean enterSetOp(ASTNode setOp) {
    try (final var ignored = withParen(true)) {
      safeVisit(setOp.get(SET_OP_LEFT));
    }

    breakLine();
    append(setOp.get(SET_OP_TYPE));

    final SetOperationOption option = setOp.get(SET_OP_OPTION);
    if (option != null) append(' ').append(option);
    breakLine();

    try (final var ignored = withParen(true)) {
      safeVisit(setOp.get(SET_OP_RIGHT));
    }

    return false;
  }

  @Override
  public boolean enterDerivedTableSource(ASTNode derivedTableSource) {
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
  public boolean enterArray(ASTNode array) {
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

  private void safeVisit(ASTNode node) {
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

  private void appendNodes(List<ASTNode> exprs, boolean withParen, boolean noBreak) {
    if (exprs == null || exprs.isEmpty()) {
      if (withParen) append("()");
      return;
    }

    if (withParen) append('(');

    for (int i = 0; i < exprs.size() - 1; i++) {
      final ASTNode expr = exprs.get(i);
      safeVisit(expr);
      append(',');
      if (noBreak) append(' ');
      else breakLine();
    }

    final ASTNode last = exprs.get(exprs.size() - 1);
    safeVisit(last);

    if (withParen) append(')');
  }

  private void appendNodes(List<ASTNode> exprs, boolean withParen) {
    appendNodes(exprs, withParen, false);
  }

  private void appendNodes(List<ASTNode> exprs) {
    appendNodes(exprs, true);
  }

  private static boolean needParen(ASTNode parent, ASTNode child, boolean isLeftChild) {
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
