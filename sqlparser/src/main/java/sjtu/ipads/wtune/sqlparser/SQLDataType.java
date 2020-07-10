package sjtu.ipads.wtune.sqlparser;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLNode.MYSQL;

public class SQLDataType {
  public enum Category {
    INTEGRAL,
    FRACTION,
    BOOLEAN,
    ENUM,
    STRING,
    BIT_STRING,
    TIME,
    BLOB,
    JSON,
    GEO,
    INTERVAL,
    NET,
    MONETARY,
    UUID,
    XML,
    RANGE,
    UNCLASSIFIED
  }

  // integral
  public static final String TINYINT = "tinyint";
  public static final String INT = "int";
  public static final String INTEGER = "integer";
  public static final String SMALLINT = "smallint";
  public static final String MEDIUMINT = "mediumint";
  public static final String BIGINT = "bigint";
  public static final String SMALLSERIAL = "smallserial";
  public static final String SERIAL = "serial";
  public static final String BIGSERIAL = "bigserial";

  // bit-string
  public static final String BIT = "bit";
  public static final String BIT_VARYING = "bit varying";

  // fraction
  public static final String REAL = "real";
  public static final String DOUBLE = "double";
  public static final String FLOAT = "float";
  public static final String DECIMAL = "decimal";
  public static final String NUMERIC = "numeric";
  public static final String FIXED = "fixed";

  // boolean
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
  public static final String TIMETZ = "timetz";
  public static final String TIMESTAMP = "timestamp";
  public static final String TIMESTAMPTZ = "timestamptz";
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
  public static final String BOX = "box";
  public static final String CIRCLE = "circle";
  public static final String LINE = "line";
  public static final String LSEG = "lseg";
  public static final String PATH = "path";

  // interval
  public static final String INTERVAL = "interval";

  // net
  public static final String CIDR = "cidr";
  public static final String INET = "inet";
  public static final String MACADDR = "MACADDR";

  // money
  public static final String MONEY = "money";

  // uuid
  public static final String UUID = "uuid";

  // xml
  public static final String XML = "xml";

  public static final String PG_LSN = "pg_lsn";
  public static final String TSVECTOR = "tsvector";
  public static final String TSQUERY = "tsquery";
  public static final String TXID_SNAPSHOT = "txid_snapshot";

  public static Set<String> TIME_TYPE =
      Set.of(YEAR, DATE, TIME, TIMETZ, TIMESTAMP, TIMESTAMPTZ, DATETIME);
  public static Set<String> FRACTION_TYPES = Set.of(REAL, DOUBLE, FLOAT, DECIMAL, NUMERIC, FIXED);
  public static Set<String> NET_TYPES = Set.of(CIDR, INET, MACADDR);
  public static Set<String> GEOMETRY_TYPES =
      Set.of(
          GEOMETRY,
          GEOMETRYCOLLECTION,
          POINT,
          MULTIPOINT,
          LINESTRING,
          MULTILINESTRING,
          POLYGON,
          MULTIPOLYGON,
          BOX,
          CIRCLE,
          LINE,
          LSEG,
          PATH);

  private final Category category;
  private final String name;
  private final int width;
  private final int precision;
  private boolean unsigned;
  private String intervalField;
  private List<String> valuesList;
  private int[] dimensions;

  private static final int[] EMPTY_INT_ARRAY = new int[0];

  public SQLDataType(Category category, String name, int width, int precision) {
    this.category = category;
    this.name = name;
    this.width = width;
    this.precision = precision;
    this.valuesList = Collections.emptyList();
    this.dimensions = EMPTY_INT_ARRAY;
  }

  public Category category() {
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

  public String intervalField() {
    return intervalField;
  }

  public List<String> valuesList() {
    return valuesList;
  }

  public int[] dimensions() {
    return dimensions;
  }

  public boolean isArray() {
    return dimensions != null && dimensions.length > 0;
  }

  private void formatTypeBody(StringBuilder builder, String dbType) {
    builder.append(name.toUpperCase());

    if (intervalField != null) builder.append(' ').append(intervalField);

    if (width != -1 && precision != -1)
      builder.append('(').append(width).append(", ").append(precision).append(')');
    else if (width != -1) builder.append('(').append(width).append(')');
    else if (precision != -1) builder.append('(').append(precision).append(')');
  }

  public void formatAsDataType(StringBuilder builder, String dbType) {
    formatTypeBody(builder, dbType);

    if (valuesList != null && !valuesList.isEmpty())
      builder.append('(').append(String.join(", ", valuesList)).append(')');

    if (dimensions != null && dimensions.length != 0)
      for (int dimension : dimensions) {
        builder.append('[');
        if (dimension != 0) builder.append(dimension);
        builder.append(']');
      }

    if (unsigned) builder.append(" UNSIGNED");
  }

  public void formatAsCastType(StringBuilder builder, String dbType) {
    if (MYSQL.equals(dbType)) {
      if (category == Category.INTEGRAL)
        if (unsigned) builder.append("UNSIGNED ");
        else builder.append("SIGNED ");

      formatTypeBody(builder, dbType);
    } else { // postgres use same rule for dataType and castType
      formatAsDataType(builder, dbType);
    }
  }

  public SQLDataType setUnsigned(boolean unsigned) {
    this.unsigned = unsigned;
    return this;
  }

  public SQLDataType setIntervalField(String intervalField) {
    this.intervalField = intervalField;
    return this;
  }

  public SQLDataType setValuesList(List<String> valuesList) {
    this.valuesList = valuesList;
    return this;
  }

  public SQLDataType setDimensions(int[] dimensions) {
    this.dimensions = dimensions;
    return this;
  }

  @Override
  public String toString() {
    //    final var builder = new StringBuilder();
    //    formatAsDataType(builder);
    //    return builder.toString();
    return null;
  }
}
