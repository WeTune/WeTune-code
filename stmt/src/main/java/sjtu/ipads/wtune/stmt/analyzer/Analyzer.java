package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.resolver.Resolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.Set;

@FunctionalInterface
public interface Analyzer<T> {
  T analyze(SQLNode node);

  default void setParam(Object... args) {}

  default T analyze(Statement stmt) {
    return analyze(stmt.parsed());
  }

  default Set<Class<? extends Resolver>> dependsOn() {
    return Collections.emptySet();
  }
}
