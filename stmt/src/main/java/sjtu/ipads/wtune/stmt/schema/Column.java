package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;

import java.util.*;

import static java.util.Collections.emptySet;
import static sjtu.ipads.wtune.common.utils.FuncUtils.find;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.*;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.simpleName;

/**
 * Column.
 *
 * <p>Note: equals and hashCode are override as only comparing [tableName, columnName]
 */
public class Column implements Attrs<Column> {
  private String tableName;
  private String columnName;
  private String rawDataType;
  private SQLDataType dataType;

  private Table table;
  private Set<Constraint> constraints;

  private static final String ATTR_PREFIX = "stmt.column.attr";

  public static final Key<Boolean> COLUMN_GENERATED =
      Attrs.key(ATTR_PREFIX + ".generated", Boolean.class);
  public static final Key<Boolean> COLUMN_HAS_DEFAULT =
      Attrs.key(ATTR_PREFIX + ".default", Boolean.class);
  public static final Key<Boolean> COLUMN_AUTOINCREMENT =
      Attrs.key(ATTR_PREFIX + ".autoIncrement", Boolean.class);
  public static final Key<Boolean> COLUMN_IS_BOOLEAN =
      Attrs.key(ATTR_PREFIX + ".isBoolean", Boolean.class);
  public static final Key<Boolean> COLUMN_IS_ENUM =
      Attrs.key(ATTR_PREFIX + ".isEnum", Boolean.class);

  public String tableName() {
    return tableName;
  }

  public String columnName() {
    return columnName;
  }

  public String rawDataType() {
    return rawDataType;
  }

  public SQLDataType dataType() {
    return dataType;
  }

  public Table table() {
    return table;
  }

  private final int[] consCachedFlags = new int[ConstraintType.values().length + 1];

  private boolean consFlag(ConstraintType type) {
    if (constraints == null) return false;
    final int ordinal = type == null ? ConstraintType.values().length : type.ordinal();

    if (consCachedFlags[ordinal] != 0) return consCachedFlags[ordinal] == 1;

    for (Constraint constraint : constraints)
      if (constraint.type() == type) {
        consCachedFlags[ordinal] = 1;
        return true;
      }

    consCachedFlags[ordinal] = -1;
    return false;
  }

  public boolean primaryKeyPart() {
    return consFlag(PRIMARY);
  }

  public boolean uniquePart() {
    return primaryKeyPart() || consFlag(UNIQUE);
  }

  public boolean foreignKeyPart() {
    return consFlag(FOREIGN);
  }

  public boolean indexPart() {
    return primaryKeyPart() || uniquePart() || foreignKeyPart() || consFlag(null);
  }

  public boolean notNull() {
    return consFlag(NOT_NULL);
  }

  public boolean hasCheck() {
    return consFlag(CHECK);
  }

  public boolean generated() {
    return isFlag(COLUMN_AUTOINCREMENT);
  }

  public boolean hasDefault() {
    return isFlag(COLUMN_HAS_DEFAULT);
  }

  public boolean autoIncrement() {
    return isFlag(COLUMN_AUTOINCREMENT);
  }

  public Set<Constraint> constraints() {
    return constraints == null ? emptySet() : constraints;
  }

  public Constraint foreignKeyConstraint() {
    return find(it -> it.type() == FOREIGN, constraints);
  }

  public void addConstraint(Constraint constraint) {
    if (constraints == null) constraints = new HashSet<>();

    assert constraint.columns().contains(this);
    constraints.add(constraint);
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public void setTable(Table table) {
    this.table = table;
    this.tableName = table.tableName();
  }

  public void setRawDataType(String rawDataType) {
    this.rawDataType = rawDataType;
  }

  public void setColumnName(String columnName) {
    this.columnName = simpleName(columnName);
  }

  public void setDataType(SQLDataType dataType) {
    this.dataType = dataType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Column column = (Column) o;
    return Objects.equals(columnName, column.columnName)
        && Objects.equals(tableName, column.tableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName, columnName);
  }

  @Override
  public String toString() {
    return String.format(
        "%s.%s",
        table() != null ? table().tableName() : tableName != null ? tableName : "??", columnName);
  }

  private final Map<String, Object> directAttrs = new HashMap<>();

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }
}
