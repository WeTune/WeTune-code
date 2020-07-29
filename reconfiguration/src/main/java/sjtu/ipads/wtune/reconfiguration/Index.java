package sjtu.ipads.wtune.reconfiguration;

import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.reconfiguration.ColumnMatching.ACCESSED_COLUMNS;

public class Index {
  private final List<Column> columns;
  private final Table table;
  private final int hash;

  private Index(List<Column> columns) {
    this.columns = columns;
    this.table = columns.get(0).table();
    this.hash = columns.hashCode();
  }

  public static Index build(List<Column> columns) {
    return new Index(columns);
  }

  public static boolean validate(List<Column> index) {
    assert !isEmpty(index);
    final Table pivotTable = index.get(0).table();
    int bytes = 0;
    for (Column column : index) {
      if (!pivotTable.equals(column.table())) return false;
      bytes += column.dataType().storageSize();
      if (bytes >= 767) return false;
    }
    return true;
  }

  public Table table() {
    return table;
  }

  public int size() {
    return columns.size();
  }

  public Column column(int i) {
    return columns.get(i);
  }

  public List<Column> columns() {
    return columns;
  }

  public List<Column> usage(Statement stmt) {
    final Set<Column> stmtColumns = stmt.get(ACCESSED_COLUMNS);
    int i = 0;
    for (; i < columns.size(); i++) if (!stmtColumns.contains(columns.get(i))) break;
    return i == 0 ? Collections.emptyList() : columns.subList(0, i);
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
}
