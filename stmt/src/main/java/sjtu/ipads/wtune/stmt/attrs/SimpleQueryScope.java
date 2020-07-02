package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.stmt.utils.StmtHelper.simpleName;

public class SimpleQueryScope extends QueryScope {
  private SQLNode specNode;

  private final Map<String, TableSource> tableSources = new HashMap<>();
  private final List<SelectItem> selectItems = new ArrayList<>();

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
  public Pair<TableSource, Boolean> resolveTable(String tableName, boolean recursive) {
    if (tableName == null) return null;
    tableName = simpleName(tableName);
    final TableSource tableSource = tableSources.get(tableName);
    if (!recursive || tableSource != null) return Pair.of(tableSource, true);
    if (parent() == null) return Pair.of(null, true);
    final Pair<TableSource, Boolean> parentResult = parent().resolveTable(tableName, true);
    return Pair.of(parentResult.left(), parentResult.left() == null);
  }

  @Override
  public TableSource resolveTable(String tableName) {
    return resolveTable(tableName, false).left();
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
    final boolean shouldRecurse = shouldRecurse();

    // qualified name. cannot be an alias
    // resolve the table source and find the column
    // (might be either a column or a select item)
    if (tableName != null) {
      final Pair<TableSource, Boolean> pair = resolveTable(tableName, shouldRecurse);
      final TableSource tableSource = pair.left();
      if (tableSource != null && tableSource.resolveRef(columnName, ref)) {
        ref.setDependent(!pair.right()); // non-local == is-dependent
        return ref;
      }
      return null;
    }

    // only if in GROUP BY, HAVING or ORDER BY can it be an alias
    // in that case, resolve it against select item first
    if (clause == SimpleQueryScope.Clause.GROUP_BY
        || clause == SimpleQueryScope.Clause.HAVING
        || clause == SimpleQueryScope.Clause.ORDER_BY) {
      final SelectItem selectItem = resolveSelection(columnName);
      if (selectItem != null) return ref.setRefItem(selectItem);
    }

    // otherwise, it must come from a table source
    // so first, look up all local table sources
    for (TableSource tableSource : tableSources().values())
      if (tableSource.resolveRef(columnName, ref)) return ref;

    // then, if recursion is permitted, resolve in parent scope
    // `clause` should be replaced by the subquery's clause.
    // e.g. when resolving `k` in SELECT * FROM a WHERE EXISTS (SELECT k),
    // this step will perform parent().resolveRef(null, "k", WHERE)
    if (!shouldRecurse) return null;

    final ColumnRef columnRef = parent().resolveRef(null, columnName, clause());
    // now it must be a dependent ref since it comes from parent
    if (columnRef != null) columnRef.setDependent(true);
    return columnRef;
  }

  /**
   * For a subquery, if it is in FROM clause, the table reference inside its scope shouldn't be
   * recursively resolved. More specifically, table reference in a derived table source can be only
   * resolved locally.
   *
   * <p>e.g. "SELECT * FROM a JOIN (SELECT a.i FROM b)" is invalid
   */
  private boolean shouldRecurse() {
    return clause() != Clause.FROM && parent() != null;
  }
}
