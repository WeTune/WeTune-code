package sjtu.ipads.wtune.reconfiguration;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import sjtu.ipads.wtune.sqlparser.SQLDataType;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Constraint;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.reconfiguration.ColumnMatching.ACCESSED_COLUMNS;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_BOOLEAN;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_ENUM;

public class Index {
  private final Set<Column> columns;
  private final Table table;
  private final int hash;

  private Index(Set<Column> columns) {
    this.columns = columns;
    this.table = Iterables.getFirst(columns, assertFalse()).table();
    this.hash = columns.hashCode();
  }

  public static Index build(List<Column> columns) {
    return new Index(new LinkedHashSet<>(columns));
  }

  public static Index build(Constraint constraint) {
    return new Index(constraint.columns());
  }

  public static boolean validate(List<Column> index) {
    assert !isEmpty(index);
    final Column firstColumn = index.get(0);
    final Table pivotTable = firstColumn.table();
    int bytes = 0;

    for (int i = 0; i < index.size(); i++) {
      final Column column = index.get(i);
      // columns are in same table?
      if (!pivotTable.equals(column.table())) return false;
      // index size < 767? (mysql limitation)
      bytes += column.dataType().storageSize();
      if (bytes >= 767) return false;
      // duplicate column?
      for (int j = 0; j < i; j++) if (column.equals(index.get(j))) return false;
    }

    final List<Column> highSelectiveColumns =
        index.stream().filter(Predicate.not(Index::isLowSelective)).collect(Collectors.toList());
    // high-selective columns + low- ones?
    if (highSelectiveColumns.size() != index.size()) return false;

    for (Constraint constraint : pivotTable.indexes())
      for (Column highSelectiveColumn : highSelectiveColumns)
        if (constraint.firstColumn().equals(highSelectiveColumn)) return false;
    return true;
  }

  public boolean coveredBy(Set<Column> otherIndex) {
    if (columns.size() > otherIndex.size()) return false;

    final Iterator<Column> iter = columns.iterator();
    final Iterator<Column> otherIter = otherIndex.iterator();
    while (iter.hasNext()) if (!iter.next().equals(otherIter.next())) return false;
    return true;
  }

  public static boolean isLowSelective(Index index) {
    final long lowSelectivePrefix =
        index.columns().stream().takeWhile(Index::isLowSelective).count();
    // index comprises 3 binary-valued (e.g. boolean) columns have 12.5% selectivity, good enough
    return lowSelectivePrefix == index.columns.size()
        || (lowSelectivePrefix > 0 && lowSelectivePrefix < 3);
  }

  public static boolean isLowSelective(Column column) {
    return guessBoolean(column); // || guessEnum(column);
  }

  private static boolean guessBoolean(Column column) {
    final String columnName = column.columnName();
    final SQLDataType dataType = column.dataType();
    return column.isFlagged(COLUMN_IS_BOOLEAN)
        || columnName.startsWith("is")
        || columnName.endsWith("ed")
        || columnName.endsWith("able")
        || columnName.endsWith("flag")
        || dataType.width() == 1
        || dataType.storageSize() == 1;
  }

  private static boolean guessEnum(Column column) {
    final String columnName = column.columnName();
    return column.isFlagged(COLUMN_IS_ENUM)
        || columnName.endsWith("type")
        || columnName.endsWith("level");
  }

  public Table table() {
    return table;
  }

  public int size() {
    return columns.size();
  }

  public Column firstColumn() {
    return Iterables.getFirst(columns, assertFalse());
  }

  public Set<Column> columns() {
    return columns;
  }

  public Set<Column> usage(Statement stmt) {
    final Set<Column> stmtColumns = stmt.get(ACCESSED_COLUMNS);
    return stmtColumns == null
        ? Collections.emptySet()
        : Sets.filter(columns, stmtColumns::contains);
  }

  public SchemaPatch toPatch() {
    final SchemaPatch patch = new SchemaPatch();
    patch.setSource("reconfig");
    patch.setTableName(table.tableName());
    patch.setColumnNames(listMap(Column::columnName, columns));
    patch.setType(SchemaPatch.Type.INDEX);
    return patch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Index index = (Index) o;
    return hash == index.hash && this.columns.equals(index.columns);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return String.format(
        "%s(%s)", table.tableName(), String.join(", ", listMap(Column::columnName, columns)));
  }
}
