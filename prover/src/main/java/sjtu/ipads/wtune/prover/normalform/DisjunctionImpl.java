package sjtu.ipads.wtune.prover.normalform;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;

final class DisjunctionImpl implements Disjunction {
  private final List<Conjunction> components;

  DisjunctionImpl(List<Conjunction> components) {
    this.components = requireNonNull(components);
  }

  public List<Conjunction> conjunctions() {
    return components;
  }

  @Override
  public String toString() {
    return joining(" + ", components);
  }
}
