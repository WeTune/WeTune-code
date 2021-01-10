package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.collector.Collector;
import sjtu.ipads.wtune.stmt.Statement;

import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

class ResolveColumnRef {
  private static final System.Logger LOG = System.getLogger("Stmt.Resolver.Column");

  public static void resolve(Statement stmt) {
    Collector.collect(stmt.parsed(), COLUMN_REF::isInstance).stream()
        .filter(not(ResolveColumnRef::resolveColumn))
        .forEach(it -> logFailure(stmt, it));
  }

  private static boolean resolveColumn(SQLNode columnRef) {
    final QueryScope scope = columnRef.get(RESOLVED_QUERY_SCOPE);
    final QueryScope.Clause clause = columnRef.get(RESOLVED_CLAUSE_SCOPE);
    final SQLNode columnIdentifier = columnRef.get(COLUMN_REF_COLUMN);
    final String tableName = columnIdentifier.get(COLUMN_NAME_TABLE);
    final String columnName = columnIdentifier.get(COLUMN_NAME_COLUMN);

    final ColumnRef ref = scope.resolveRef(tableName, columnName, clause);
    if (ref == null) {
      scope.resolveRef(tableName, columnName, clause);
      return false;
    }

    columnRef.put(RESOLVED_COLUMN_REF, ref.setNode(columnRef));
    return true;
  }

  private static void logFailure(Statement stmt, SQLNode columnRef) {
    LOG.log(
        System.Logger.Level.WARNING,
        "unresolved column {2}\n{0}\n{1}",
        stmt,
        stmt.parsed().toString(),
        columnRef);
  }
}
