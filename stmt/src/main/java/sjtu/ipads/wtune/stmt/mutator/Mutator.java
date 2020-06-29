package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.stmt.resovler.Resolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.Set;

public interface Mutator {
  void mutate(Statement stmt);

  default Set<Class<? extends Resolver>> dependsOnResolver() {
    return Collections.emptySet();
  }
}
