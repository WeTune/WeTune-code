package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.tree.AstNode;

public interface SqlNode extends AstNode<SqlKind> {
  String MySQL = "mysql";
  String PostgreSQL = "postgresql";
  String SQLServer = "sqlserver";

  @Override
  SqlContext context();

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
