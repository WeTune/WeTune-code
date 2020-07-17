package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.utils.StmtHelper;

import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeHash;

public class SelectItem {
  private SQLNode node;
  private SQLNode expr;
  private String simpleName;
  private String alias;

  private boolean isPrimary;

  public static SelectItem fromNode(SQLNode node) {
    assert node.type() == SQLNode.Type.SELECT_ITEM;

    final String alias = node.get(SELECT_ITEM_ALIAS);
    final SQLNode expr = node.get(SELECT_ITEM_EXPR);

    final SelectItem item = new SelectItem();
    item.setNode(node);
    item.setExpr(expr);
    item.setAlias(alias);
    if (exprKind(expr) == SQLExpr.Kind.COLUMN_REF)
      item.setSimpleName(expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN));

    return item;
  }

  public static SelectItem selectInputColumn(TableSource source, Column column, String alias) {
    final SQLNode refNode = SQLExpr.columnRef(source.name(), column.columnName());

    final ColumnRef cRef = new ColumnRef();
    cRef.setNode(refNode);
    cRef.setSource(source);
    cRef.setRefColumn(column);
    refNode.put(RESOLVED_COLUMN_REF, cRef);

    return SelectItem.fromNode(selectItem(refNode, alias));
  }

  public static SelectItem selectOutputColumn(
      TableSource source, SelectItem subItem, String alias) {
    final SQLNode refNode =
        SQLExpr.columnRef(source.name(), coalesce(subItem.alias(), subItem.simpleName()));

    final ColumnRef cRef = new ColumnRef();
    cRef.setNode(refNode);
    cRef.setSource(source);
    cRef.setRefItem(subItem);

    refNode.put(RESOLVED_COLUMN_REF, cRef);

    return SelectItem.fromNode(selectItem(refNode, alias));
  }

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

  public boolean isPrimary() {
    return isPrimary;
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

  public void setPrimary(boolean primary) {
    isPrimary = primary;
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
