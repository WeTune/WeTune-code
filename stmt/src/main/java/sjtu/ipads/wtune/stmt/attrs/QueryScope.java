package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_CLAUSE_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public abstract class QueryScope {
  public enum Clause {
    SELECT_ITEM,
    FROM,
    ON,
    WHERE,
    HAVING,
    ORDER_BY,
    GROUP_BY,
    LIMIT,
    OFFSET
  }

  private QueryScope parent;
  private SQLNode queryNode;

  public QueryScope parent() {
    return parent;
  }

  public SQLNode queryNode() {
    return queryNode;
  }

  public SQLNode leftChild() {
    return null;
  }

  public SQLNode rightChild() {
    return null;
  }

  public int level() {
    if (parent == null) return 0;
    return parent.level() + 1;
  }

  public Map<String, TableSource> tableSources() {
    return Collections.emptyMap();
  }

  public List<SelectItem> selectItems() {
    return Collections.emptyList();
  }

  public Clause clause() {
    return queryNode().get(RESOLVED_CLAUSE_SCOPE);
  }

  public void setLeftChild(SQLNode child) {}

  public void setRightChild(SQLNode child) {}

  public void setSpecNode(SQLNode specNode) {}

  public void setParent(QueryScope parent) {
    this.parent = parent;
  }

  public void setQueryNode(SQLNode queryNode) {
    this.queryNode = queryNode;
  }

  public void addTable(TableSource tableSource) {}

  public void addSelectItem(SelectItem item) {}

  public void setScope(SQLNode node) {
    if (node == null) return;
    node.put(RESOLVED_QUERY_SCOPE, this);
    node.children().forEach(this::setScope);
  }

  /** @return table-source, is-local */
  public Pair<TableSource, Boolean> resolveTable(String tableName, boolean recursive) {
    return Pair.of(null, false);
  }

  public TableSource resolveTable(String tableName) {
    return null;
  }

  public SelectItem resolveSelection(String name) {
    return null;
  }

  public ColumnRef resolveRef(String tableName, String columnName, Clause clause) {
    return null;
  }
}
