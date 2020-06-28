package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.HashMap;
import java.util.Map;

public class QueryScope {
  private QueryScope parent;
  private SQLNode queryNode;
  private SQLNode firstSpecNode;

  private Map<String, TableSource> tableSources = new HashMap<>();

  public QueryScope parent() {
    return parent;
  }

  public int level() {
    if (parent == null) return 0;
    return parent.level() + 1;
  }

  public SQLNode queryNode() {
    return queryNode;
  }

  public SQLNode firstSpecNode() {
    return firstSpecNode;
  }

  public Map<String, TableSource> tableSources() {
    return tableSources;
  }

  public void setParent(QueryScope parent) {
    this.parent = parent;
  }

  public void setQueryNode(SQLNode queryNode) {
    this.queryNode = queryNode;
  }

  public void setFirstSpecNode(SQLNode firstSpecNode) {
    this.firstSpecNode = firstSpecNode;
  }

  public void addTable(TableSource tableSource) {
    tableSources.put(tableSource.name, tableSource);
  }
}
