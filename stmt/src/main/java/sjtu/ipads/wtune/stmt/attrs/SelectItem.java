package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.utils.StmtHelper;

import java.util.Objects;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeHash;

public class SelectItem {
  private SQLNode node;
  private SQLNode expr;
  private String simpleName;
  private String alias;

  public SQLNode node() {
    return node;
  }

  public SQLNode expr() {
    return expr;
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

  public void setExpr(SQLNode expr) {
    this.expr = expr;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SelectItem item = (SelectItem) o;
    return nodeEquals(node, item.node);
  }

  @Override
  public int hashCode() {
    return nodeHash(node);
  }
}
