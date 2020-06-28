package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.Set;

@FunctionalInterface
public interface Resolver {
  void resolve(Statement stmt);

  default Set<Class<? extends Resolver>> dependsOn() {
    return Collections.emptySet();
  }
}
