package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.resolver.Resolver;
import sjtu.ipads.wtune.stmt.resolver.TableResolver;
import sjtu.ipads.wtune.stmt.schema.Table;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_TABLE_SOURCE;

public class TableAccessAnalyzer implements SQLVisitor, Analyzer<Set<Table>> {
  private final Set<Table> tables = new HashSet<>();

  @Override
  public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
    final Table table = simpleTableSource.get(RESOLVED_TABLE_SOURCE).table();
    if (table != null) tables.add(table);
    return false;
  }

  @Override
  public Set<Table> analyze(SQLNode node) {
    node.accept(this);
    return tables;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(TableResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
