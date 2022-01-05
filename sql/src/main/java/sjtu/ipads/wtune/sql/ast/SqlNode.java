package sjtu.ipads.wtune.sql.ast;

import sjtu.ipads.wtune.common.tree.LabeledTreeNode;

import static sjtu.ipads.wtune.sql.ast.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast.SqlKind.TableSource;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.Expr_Kind;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.TableSource_Kind;

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

  static SqlNode mk(SqlContext ctx, ExprKind kind) {
    final SqlNode expr = mk(ctx, ctx.mkNode(Expr));
    expr.$(Expr_Kind, kind);
    return expr;
  }

  static SqlNode mk(SqlContext ctx, TableSourceKind kind) {
    final SqlNode tableSource = mk(ctx, TableSource);
    tableSource.$(TableSource_Kind, kind);
    return tableSource;
  }
}
