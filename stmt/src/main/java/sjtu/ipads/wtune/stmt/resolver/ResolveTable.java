package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.stmt.collector.Collector;
import sjtu.ipads.wtune.stmt.schema.Schema;
import sjtu.ipads.wtune.stmt.schema.Table;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_TABLE_SOURCE;

class ResolveTable {
  public static void resolve(Statement stmt) {
    final Schema schema = App.find(stmt.appName()).schema("base");
    Collector.collect(stmt.parsed(), SIMPLE_SOURCE::isInstance).forEach(it -> resolveSimple(schema, it));
    Collector.collect(stmt.parsed(), DERIVED_SOURCE::isInstance).forEach(ResolveTable::resolveDerived);
  }

  private static void resolveSimple(Schema schema, SQLNode node) {
    final String alias = node.get(SIMPLE_ALIAS);
    final String tableName = node.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE);
    final Table table = schema.getTable(tableName);

    if (table == null) return;

    final TableSource tableSource = new TableSource();
    tableSource.setNode(node);
    tableSource.setName(alias != null ? alias : table.tableName());
    tableSource.setTable(table);

    node.put(RESOLVED_TABLE_SOURCE, tableSource);
    node.get(RESOLVED_QUERY_SCOPE).addTable(tableSource);
  }

  private static void resolveDerived(SQLNode node) {
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    final String alias = node.get(DERIVED_ALIAS);

    final TableSource tableSource = new TableSource();
    tableSource.setNode(node);
    tableSource.setTable(null);
    tableSource.setName(
        alias != null // in fact it shouldn't be null
            ? alias
            : String.format("_sub_%s_%s", scope.level(), scope.tableSources().size()));

    node.put(RESOLVED_TABLE_SOURCE, tableSource);
    scope.addTable(tableSource);
  }
}
