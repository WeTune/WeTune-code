package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.tree.AstNodeBase;

class SqlNodeImpl extends AstNodeBase<SqlKind> implements SqlNode {
  SqlNodeImpl(SqlContext context, int nodeId) {
    super(context, nodeId);
  }

  @Override
  public SqlContext context() {
    return (SqlContext) super.context();
  }

  @Override
  public void accept(SqlVisitor visitor) {
    if (SqlVisitorDriver.enter(this, visitor)) SqlVisitorDriver.visitChildren(this, visitor);
    SqlVisitorDriver.leave(this, visitor);
  }

  @Override
  public String toString() {
    return toString(true);
  }

  @Override
  public String toString(boolean oneLine) {
    final SqlFormatter formatter = new SqlFormatter(oneLine);
    accept(formatter);
    return formatter.toString();
  }
}
