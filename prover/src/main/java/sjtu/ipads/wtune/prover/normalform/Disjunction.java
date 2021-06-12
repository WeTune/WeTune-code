package sjtu.ipads.wtune.prover.normalform;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;

final class Disjunction {
  private final List<Conjunction> components;

  Disjunction(List<Conjunction> components) {
    this.components = requireNonNull(components);
  }

  public List<Conjunction> components() {
    return components;
  }

  @Override
  public String toString() {
    return joining(" + ", components);
  }
}
