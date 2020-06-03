package sjtu.ipads.wtune.sqlparser;

import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Constraint.*;

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

  private void insertIndent() {
    for (int i = 0; i < indent; i++) builder.append(INDENT_STR);
  }

  private void breakLineAndIndent() {
    breakLine();
    ++indent;
  }

  private void breakLine() {
    builder.append('\n');
  }

  @Override
  public boolean enterCreateTable(SQLNode createTable) {
    builder.append("CREATE TABLE ");

    final var tableName = createTable.get(CREATE_TABLE_NAME);
    tableName.accept(this);

    builder.append(" (");
    if (!oneLine) breakLineAndIndent();

    for (var colDefs : createTable.get(CREATE_TABLE_COLUMNS)) {
      if (!oneLine) insertIndent();
      colDefs.accept(this);
      builder.append(',');
      if (!oneLine) breakLine();
    }

    for (var conDefs : createTable.get(CREATE_TABLE_CONSTRAINTS)) {
      if (!oneLine) insertIndent();
      conDefs.accept(this);
      builder.append(',');
      if (!oneLine) breakLine();
    }

    builder.delete(builder.length() - (oneLine ? 1 : 2), builder.length() - (oneLine ? 0 : 1));
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
    final var colName = colDef.get(COLUMN_DEF_NAME);
    colName.accept(this);
    builder.append(' ').append(colDef.get(COLUMN_DEF_DATATYPE));

    if (colDef.isFlagged(COLUMN_DEF_CONS, UNIQUE)) builder.append(" UNIQUE");
    if (colDef.isFlagged(COLUMN_DEF_CONS, PRIMARY)) builder.append(" PRIMARY KEY");
    if (colDef.isFlagged(COLUMN_DEF_CONS, NOT_NULL)) builder.append(" NOT NULL");
    if (colDef.isFlagged(COLUMN_DEF_AUTOINCREMENT)) builder.append(" AUTO_INCREMENT");

    final var references = colDef.get(COLUMN_DEF_REF);
    if (references != null) references.accept(this);

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
    ref.get(REFERENCES_TABLE).accept(this);

    final var columns = ref.get(REFERENCES_COLUMNS);
    if (columns != null) {
      builder.append('(');
      for (SQLNode column : columns) {
        column.accept(this);
        builder.append(", ");
      }
      builder.delete(builder.length() - 2, builder.length());
      builder.append(')');
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
        case SPATIAL:
          builder.append("SPATIAL ");
      }

    builder.append("KEY ");

    if (name != null) builder.append('`').append(name).append('`');

    builder.append('(');
    for (SQLNode key : keys) {
      key.accept(this);
      builder.append(", ");
    }
    builder.delete(builder.length() - 2, builder.length());
    builder.append(')');

    if (refs != null) refs.accept(this);

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
    final var columnName = keyPart.get(KEY_PART_COLUMN);
    final var length = keyPart.get(KEY_PART_LEN);
    final var direction = keyPart.get(KEY_PART_DIRECTION);

    builder.append('`').append(columnName).append('`');
    if (length != null) builder.append('(').append(length).append(')');
    if (direction != null) builder.append(' ').append(direction);

    return false;
  }

  public String toString() {
    return builder.toString();
  }
}
