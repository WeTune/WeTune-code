package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.tree.LabeledTreeNode;

public interface SqlNode extends LabeledTreeNode<SqlKind, SqlContext, SqlNode> {
  String MySQL = "mysql";
  String PostgreSQL = "postgresql";
  String SQLServer = "sqlserver";

  void accept(SqlVisitor visitor);

  String toString(boolean oneLine);

  default String dbType() {
    return context().dbType();
  }

  static SqlNode mk(SqlContext ctx, int nodeId) {
    return new SqlNodeImpl(ctx, nodeId);
  }

  static SqlNode mk(SqlContext ctx, SqlKind kind) {
    return mk(ctx, ctx.mkNode(kind));
  }
}
