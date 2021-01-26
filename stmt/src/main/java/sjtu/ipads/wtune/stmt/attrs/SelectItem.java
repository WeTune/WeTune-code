package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.utils.StmtHelper;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;

public class SelectItem {
  private SQLNode node;
  private SQLNode expr;
  private String simpleName;
  private String alias;

  public static SelectItem fromNode(SQLNode node) {
    assert SELECT_ITEM.isInstance(node);

    final String alias = node.get(SELECT_ITEM_ALIAS);
    final SQLNode expr = node.get(SELECT_ITEM_EXPR);

    final SelectItem item = new SelectItem();
    item.setNode(node);
    item.setExpr(expr);
    item.setAlias(alias);
    if (COLUMN_REF.isInstance(expr))
      item.setSimpleName(expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN));

    return item;
  }

  public SQLNode node() {
    return node;
  }

  public SQLNode expr() {
    return expr;
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

  public String alias() {
    return alias;
  }

  public String simpleName() {
    return simpleName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SelectItem item = (SelectItem) o;
    return node == item.node;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(node);
  }
}
