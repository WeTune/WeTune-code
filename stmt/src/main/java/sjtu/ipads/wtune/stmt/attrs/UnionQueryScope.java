package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class UnionQueryScope extends QueryScope {
  private SQLNode left;
  private SQLNode right;

  @Override
  public void setLeftChild(SQLNode child) {
    this.left = child;
  }

  @Override
  public void setRightChild(SQLNode child) {
    this.right = child;
  }

  @Override
  public SQLNode leftChild() {
    return left;
  }

  @Override
  public SQLNode rightChild() {
    return right;
  }

  private QueryScope leftScope() {
    final QueryScope leftScope = left.get(RESOLVED_QUERY_SCOPE);
    assert leftScope != this;
    return leftScope;
  }

  @Override
  public List<SelectItem> selectItems() {
    return leftScope().selectItems();
  }

  @Override
  public SelectItem resolveSelection(String name) {
    return leftScope().resolveSelection(name);
  }

  @Override
  public ColumnRef resolveRef(String tableName, String columnName, Clause clause) {
    // in UNION a column ref can only appear in ORDER BY
    // in that case, it must be an alias in the left-most query
    // see: https://dev.mysql.com/doc/refman/5.7/en/union.html#union-order-by-limit
    if (tableName != null) return null;
    return leftScope().resolveRef(null, columnName, clause);
  }
}
