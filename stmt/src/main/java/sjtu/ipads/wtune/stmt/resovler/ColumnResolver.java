package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.lang.System.Logger.Level;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

public class ColumnResolver implements Resolver, SQLVisitor {
  private static final System.Logger LOG = System.getLogger("Stmt.Resolver.Column");

  private Statement stmt;

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    final QueryScope scope = columnRef.get(RESOLVED_QUERY_SCOPE);
    final QueryScope.Clause clause = columnRef.get(RESOLVED_CLAUSE_SCOPE);
    final SQLNode columnIdentifier = columnRef.get(COLUMN_REF_COLUMN);
    final String tableName = columnIdentifier.get(COLUMN_NAME_TABLE);
    final String columnName = columnIdentifier.get(COLUMN_NAME_COLUMN);

    final ColumnRef ref = scope.resolveRef(tableName, columnName, clause);
    if (ref != null) columnRef.put(RESOLVED_COLUMN_REF, ref.setNode(columnRef));
    else
      LOG.log(
          Level.WARNING,
          "failed to resolve column ref {3}\n<{0}, {1}>\n{2}",
          stmt.appName(),
          stmt.stmtId(),
          stmt.parsed().toString(false),
          columnRef);

    return false;
  }

  @Override
  public void resolve(Statement stmt) {
    LOG.log(
        System.Logger.Level.TRACE,
        "resolving column for <{0}, {1}>",
        stmt.appName(),
        stmt.stmtId());
    this.stmt = stmt;
    stmt.parsed().accept(this);
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(QueryScopeResolver.class, TableResolver.class, SelectionResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
