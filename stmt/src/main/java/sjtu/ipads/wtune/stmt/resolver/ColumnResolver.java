package sjtu.ipads.wtune.stmt.resolver;

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

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(QueryScopeResolver.class, TableResolver.class, SelectionResolver.class);

  private Statement stmt;
  private boolean isAllSuccessful = true;

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    final QueryScope scope = columnRef.get(RESOLVED_QUERY_SCOPE);
    final QueryScope.Clause clause = columnRef.get(RESOLVED_CLAUSE_SCOPE);
    final SQLNode columnIdentifier = columnRef.get(COLUMN_REF_COLUMN);
    final String tableName = columnIdentifier.get(COLUMN_NAME_TABLE);
    final String columnName = columnIdentifier.get(COLUMN_NAME_COLUMN);

    final ColumnRef ref = scope.resolveRef(tableName, columnName, clause);

    if (ref != null) columnRef.put(RESOLVED_COLUMN_REF, ref.setNode(columnRef));
    else {
      LOG.log(
          Level.WARNING,
          "unresolved column {2}\n{0}\n{1}",
          stmt,
          stmt.parsed().toString(false),
          columnRef);
      isAllSuccessful = false;
    }

    return false;
  }

  @Override
  public boolean resolve(Statement stmt) {
    this.stmt = stmt;
    stmt.parsed().accept(this);
    return isAllSuccessful;
  }
}
