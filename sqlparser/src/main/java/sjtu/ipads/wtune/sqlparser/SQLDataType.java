package sjtu.ipads.wtune.sqlparser;

import java.util.List;

public class SQLDataType {
  // integral
  public static final String TINYINT = "tinyint";
  public static final String INT = "int";
  public static final String SMALLINT = "smallint";
  public static final String MEDIUMINT = "mediumint";
  public static final String BIGINT = "bigint";
  public static final String BIT = "bit";
  public static final String SERIAL = "serial";

  // fraction
  public static final String REAL = "real";
  public static final String DOUBLE = "double";
  public static final String FLOAT = "float";
  public static final String DECIMAL = "decimal";
  public static final String NUMERIC = "numeric";
  public static final String FIXED = "fixed";

  // boolean
  public static final String BOOL = "bool";
  public static final String BOOLEAN = "boolean";

  // enum
  public static final String ENUM = "enum";
  public static final String SET = "set";

  // string
  public static final String CHAR = "char";
  public static final String VARCHAR = "varchar";
  public static final String BINARY = "binary";
  public static final String VARBINARY = "varbinary";
  public static final String TINYTEXT = "tinytext";
  public static final String TEXT = "text";
  public static final String MEDIUMTEXT = "mediumtext";
  public static final String BIGTEXT = "bigtext";

  // time
  public static final String YEAR = "year";
  public static final String DATE = "date";
  public static final String TIME = "time";
  public static final String TIMESTAMP = "timestamp";
  public static final String DATETIME = "datetime";

  // blob
  public static final String TINYBLOB = "tinyblob";
  public static final String BLOB = "blob";
  public static final String MEDIUMBLOB = "mediumblob";
  public static final String LONGBLOB = "longblob";

  // json
  public static final String JSON = "json";

  // geo
  public static final String GEOMETRY = "geometry";
  public static final String GEOMETRYCOLLECTION = "geometrycollection";
  public static final String POINT = "point";
  public static final String MULTIPOINT = "multipoint";
  public static final String LINESTRING = "linestring";
  public static final String MULTILINESTRING = "multilinestring";
  public static final String POLYGON = "polygon";
  public static final String MULTIPOLYGON = "multipolygon";

  private final SQLDataCategory category;
  private final String name;
  private final int width;
  private final int precision;
  private final boolean unsigned;
  private final List<String> valuesList;

  public SQLDataType(
      SQLDataCategory category,
      String name,
      int width,
      int precision,
      boolean unsigned,
      List<String> valuesList) {
    this.category = category;
    this.name = name;
    this.width = width;
    this.precision = precision;
    this.unsigned = unsigned;
    this.valuesList = valuesList;
  }

  public SQLDataCategory category() {
    return category;
  }

  public String name() {
    return name;
  }

  public int width() {
    return width;
  }

  public int precision() {
    return precision;
  }

  public boolean unsigned() {
    return unsigned;
  }

  public void format(StringBuilder builder) {
    builder.append(name);
    if (width != -1 && precision != -1)
      builder.append('(').append(width).append(", ").append(precision).append(')');
    else if (width != -1) builder.append('(').append(width).append(')');
    else if (precision != -1) builder.append('(').append(precision).append(')');

    if (valuesList != null && !valuesList.isEmpty())
      builder.append('(').append(String.join(", ", valuesList)).append(')');

    if (unsigned) builder.append(" UNSIGNED");
  }

  @Override
  public String toString() {
    final var builder = new StringBuilder();
    format(builder);
    return builder.toString();
  }
}
