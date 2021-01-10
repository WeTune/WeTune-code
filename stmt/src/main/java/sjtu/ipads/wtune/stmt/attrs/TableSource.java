package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Table;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.simpleName;

public class TableSource {
  private SQLNode node;
  private String name;
  private Table table;

  /** Name of the table source (not null). Either the alias or the table's name. */
  public String name() {
    return name;
  }

  public SQLNode node() {
    return node;
  }

  public Table table() {
    return table;
  }

  public boolean isDerived() {
    return table == null;
  }

  public void setName(String name) {
    this.name = simpleName(name);
  }

  public void setNode(SQLNode node) {
    this.node = node;
  }

  public void setTable(Table table) {
    this.table = table;
  }

  public List<String> namedSelections() {
    if (isDerived()) {
      return node.get(DERIVED_SUBQUERY).get(RESOLVED_QUERY_SCOPE).selectItems().stream()
          .map(it -> coalesce(it.alias(), it.simpleName()))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

    } else return listMap(Column::columnName, table.columns());
  }

  public boolean resolveRef(String columnName, ColumnRef ref) {
    if (isDerived()) {
      final SelectItem item = resolveAsSelection(columnName);
      if (item == null) return false;
      ref.setRefItem(item);

    } else {
      final Column column = resolveAsColumn(columnName);
      if (column == null) return false;
      ref.setRefColumn(column);
    }
    ref.setSource(this);
    return true;
  }

  public Column resolveAsColumn(String name) {
    if (name == null || table == null) return null;
    return table.getColumn(name);
  }

  public SelectItem resolveAsSelection(String name) {
    if (name == null || node == null) return null;
    return node.get(DERIVED_SUBQUERY).get(RESOLVED_QUERY_SCOPE).resolveSelection(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TableSource that = (TableSource) o;
    return node == that.node;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(node);
  }

  @Override
  public String toString() {
    return node.toString();
  }
}
