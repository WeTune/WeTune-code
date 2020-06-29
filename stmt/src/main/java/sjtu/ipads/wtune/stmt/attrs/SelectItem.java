package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.utils.StmtHelper;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;

public class SelectItem {
  private SQLNode node;
  private String simpleName;
  private String alias;

  public SQLNode node() {
    return node;
  }

  public String simpleName() {
    return simpleName;
  }

  public String alias() {
    return alias;
  }

  public SelectItem setNode(SQLNode node) {
    this.node = node;
    return this;
  }

  public SelectItem setSimpleName(String simpleName) {
    this.simpleName = StmtHelper.simpleName(simpleName);
    return this;
  }

  public SelectItem setAlias(String alias) {
    this.alias = StmtHelper.simpleName(alias);
    return this;
  }

  public boolean isWildcard() {
    return exprKind(node) == SQLExpr.Kind.WILDCARD;
  }
}
