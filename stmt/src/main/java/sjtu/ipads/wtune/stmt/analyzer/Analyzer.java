package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.stmt.resovler.Resolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.Set;

@FunctionalInterface
public interface Analyzer<T> {
  T analyze(Statement stmt);

  default Set<Class<? extends Resolver>> dependsOn() {
    return Collections.emptySet();
  }
}
