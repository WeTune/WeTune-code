package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.stmt.utils.StmtHelper.simpleName;

public class SimpleQueryScope extends QueryScope {
  private SQLNode specNode;

  private Map<String, TableSource> tableSources = new HashMap<>();
  private List<SelectItem> selectItems = new ArrayList<>();

  public SQLNode specNode() {
    return specNode;
  }

  @Override
  public Map<String, TableSource> tableSources() {
    return tableSources;
  }

  @Override
  public void setSpecNode(SQLNode specNode) {
    this.specNode = specNode;
  }

  @Override
  public void addTable(TableSource tableSource) {
    tableSources.put(tableSource.name(), tableSource);
  }

  @Override
  public void addSelectItem(SelectItem item) {
    selectItems.add(item);
  }

  @Override
  public TableSource resolveTable(String tableName) {
    if (tableName == null) return null;
    tableName = simpleName(tableName);
    final TableSource tableSource = tableSources.get(tableName);
    return tableSource != null
        ? tableSource
        : parent() != null ? parent().resolveTable(tableName) : null;
  }

  @Override
  public SelectItem resolveSelection(String name) {
    if (name == null) return null;
    name = simpleName(name);

    for (SelectItem item : selectItems)
      if (name.equals(item.alias()) || name.equals(item.simpleName())) return item;

    return null;
  }

  @Override
  public ColumnRef resolveRef(String tableName, String columnName, Clause clause) {
    final ColumnRef ref = new ColumnRef();

    // qualified name, cannot be an alias
    if (tableName != null) {
      final TableSource tableSource = resolveTable(tableName);
      return tableSource != null && tableSource.resolveRef(columnName, ref) ? ref : null;
    }

    // only if in GROUP BY, HAVING or ORDER BY can it be an alias
    if (clause == SimpleQueryScope.Clause.GROUP_BY
        || clause == SimpleQueryScope.Clause.HAVING
        || clause == SimpleQueryScope.Clause.ORDER_BY) {
      final SelectItem selectItem = resolveSelection(columnName);
      if (selectItem != null) return ref.setRefItem(selectItem);
    }

    // lookup column from all table sources
    for (TableSource tableSource : tableSources().values())
      if (tableSource.resolveRef(columnName, ref)) return ref;

    return null;
  }
}
