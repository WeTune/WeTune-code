package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.resovler.QueryScopeResolver;
import sjtu.ipads.wtune.stmt.resovler.Resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class QueryCollector implements Analyzer<List<SQLNode>>, SQLVisitor {
  private final List<SQLNode> subqueries = new ArrayList<>();
  private QueryScope rootQueryScope;
  private boolean subqueryOnly = true;
  private boolean shortCut = false;
  private boolean stop = false;

  @Override
  public boolean enter(SQLNode node) {
    return !stop;
  }

  @Override
  public boolean enterQuery(SQLNode query) {
    final QueryScope scope = query.get(RESOLVED_QUERY_SCOPE);
    if (!rootQueryScope.equals(scope)) {
      subqueries.add(query);
      if (shortCut) stop = true;
      return false;
    }
    return true;
  }

  @Override
  public void setParam(Object... args) {
    subqueryOnly = (boolean) args[0];
    shortCut = (boolean) args[1];
  }

  @Override
  public List<SQLNode> analyze(SQLNode node) {
    stop = false;
    rootQueryScope = node.get(RESOLVED_QUERY_SCOPE);
    if (rootQueryScope == null) return Collections.emptyList();
    node.accept(this);
    return subqueries;
  }

  public static List<SQLNode> collect(SQLNode node, boolean subqueryOnly) {
    final QueryCollector collector = new QueryCollector();
    collector.subqueryOnly = subqueryOnly;
    return collector.analyze(node);
  }


  public static boolean hasSubquery(SQLNode node) {
    final QueryCollector collector = new QueryCollector();
    collector.subqueryOnly = true;
    collector.shortCut = true;
    return collector.analyze(node).size() != 0;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(QueryScopeResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
