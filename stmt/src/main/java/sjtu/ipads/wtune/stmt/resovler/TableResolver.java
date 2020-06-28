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

public class TableResolver implements SQLVisitor, Resolver {
  public static System.Logger LOG = System.getLogger("Stmt.Resolver.Table");
  private Schema schema;
  private Statement stmt;

  @Override
  public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    final QueryScope queryScope = derivedTableSource.get(RESOLVED_QUERY_SCOPE);

    final String alias = derivedTableSource.get(DERIVED_ALIAS);

    final TableSource tableSource = new TableSource();
    tableSource.setTableSource(derivedTableSource);
    tableSource.setTable(null);
    tableSource.setName(
        alias != null
            ? alias
            : String.format("_sub_%s_%s", queryScope.level(), queryScope.tableSources().size()));

    queryScope.addTable(tableSource);
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
      return false;
    }

    final TableSource tableSource = new TableSource();
    tableSource.setTableSource(simpleTableSource);
    tableSource.setName(alias != null ? alias : table.tableName());
    tableSource.setTable(table);

    simpleTableSource.get(RESOLVED_QUERY_SCOPE).addTable(tableSource);

    return false;
  }

  @Override
  public void resolve(Statement stmt) {
    this.stmt = stmt;
    this.schema = stmt.appContext().schema();
    stmt.parsed().accept(this);
  }

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return Collections.singleton(QueryScopeResolver.class);
  }
}
