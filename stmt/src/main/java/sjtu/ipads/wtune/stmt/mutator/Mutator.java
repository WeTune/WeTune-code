package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.stmt.resolver.Resolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface Mutator {
  void mutate(Statement stmt);

  default Set<Class<? extends Resolver>> dependsOnResolver() {
    return Collections.emptySet();
  }

  List<Class<? extends Mutator>> STANDARD_MUTATORS =
      List.of(Cleaner.class, BoolNormalizer.class, ConstantTableNormalizer.class);
}
