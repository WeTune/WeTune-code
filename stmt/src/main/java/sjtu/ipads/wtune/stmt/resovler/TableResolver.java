package sjtu.ipads.wtune.stmt.resovler;

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
    final QueryScope simpleQueryScope = derivedTableSource.get(RESOLVED_QUERY_SCOPE);

    final String alias = derivedTableSource.get(DERIVED_ALIAS);

    final TableSource tableSource = new TableSource();
    tableSource.setNode(derivedTableSource);
    tableSource.setTable(null);
    tableSource.setName(
        alias != null // in fact it shouldn't be null
            ? alias
            : String.format(
                "_sub_%s_%s", simpleQueryScope.level(), simpleQueryScope.tableSources().size()));

    simpleQueryScope.addTable(tableSource);
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
          "failed to resolve table {0} in {1}\n<{2}, {3}>:\n{4}",
          tableName,
          simpleTableSource,
          stmt.appName(),
          stmt.stmtId(),
          stmt.parsed().toString(false));
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
    LOG.log(
        System.Logger.Level.TRACE, "resolving table for <{0}, {1}>", stmt.appName(), stmt.stmtId());

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
