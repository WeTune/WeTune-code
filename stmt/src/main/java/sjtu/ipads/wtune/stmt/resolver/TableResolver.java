package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.stmt.schema.Schema;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLNode.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_TABLE_SOURCE;

public class TableResolver implements SQLVisitor, Resolver {
  public static System.Logger LOG = System.getLogger("Stmt.Resolver.Table");
  private Schema schema;
  private Statement stmt;
  private boolean isAllSuccessful = true;

  @Override
  public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    final QueryScope scope = derivedTableSource.get(RESOLVED_QUERY_SCOPE);

    final String alias = derivedTableSource.get(DERIVED_ALIAS);

    final TableSource tableSource = new TableSource();
    tableSource.setNode(derivedTableSource);
    tableSource.setTable(null);
    tableSource.setName(
        alias != null // in fact it shouldn't be null
            ? alias
            : String.format("_sub_%s_%s", scope.level(), scope.tableSources().size()));

    derivedTableSource.put(RESOLVED_TABLE_SOURCE, tableSource);
    scope.addTable(tableSource);
    return true;
  }

  @Override
  public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
    final String alias = simpleTableSource.get(SIMPLE_ALIAS);
    final String tableName = simpleTableSource.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE);
    final Table table = schema.getTable(tableName);

    if (table == null) {
      LOG.log(
          System.Logger.Level.WARNING,
          "unresolved table {2} in {3}\n<{0}>:\n{1}",
          stmt,
          stmt.parsed().toString(false),
          tableName,
          simpleTableSource);
      isAllSuccessful = false;
      return false;
    }

    final TableSource tableSource = new TableSource();
    tableSource.setNode(simpleTableSource);
    tableSource.setName(alias != null ? alias : table.tableName());
    tableSource.setTable(table);

    simpleTableSource.put(RESOLVED_TABLE_SOURCE, tableSource);
    simpleTableSource.get(RESOLVED_QUERY_SCOPE).addTable(tableSource);

    return false;
  }

  @Override
  public boolean resolve(Statement stmt) {
    this.stmt = stmt;
    this.schema = stmt.appContext().schema();

    stmt.parsed().accept(this);
    return isAllSuccessful;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Collections.singleton(QueryScopeResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
