package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.resovler.ColumnResolver;
import sjtu.ipads.wtune.stmt.resovler.Resolver;

import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class DependentQueryAnalyzer implements Analyzer<Boolean>, SQLVisitor {
  private boolean isDependent = false;

  @Override
  public Boolean analyze(SQLNode node) {
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    scope.queryNode().accept(this);
    return isDependent;
  }

  @Override
  public boolean enter(SQLNode node) {
    return !isDependent;
  }

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    if (columnRef.get(RESOLVED_COLUMN_REF).isDependent()) isDependent = true;
    return false;
  }

  public static boolean isDependent(SQLNode node) {
    return new DependentQueryAnalyzer().analyze(node);
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(ColumnResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
