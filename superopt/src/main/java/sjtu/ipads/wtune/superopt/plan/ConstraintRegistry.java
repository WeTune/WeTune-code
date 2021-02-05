package sjtu.ipads.wtune.superopt.plan;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;

public interface ConstraintRegistry {
  Placeholder[] sourceOf(Placeholder pick);

  Collection<Placeholder> eqPicksOf(Placeholder pick);

  Collection<Placeholder> eqInputOf(Placeholder input);

  Collection<Pair<Placeholder, Placeholder>> references();
}
