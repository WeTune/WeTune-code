package sjtu.ipads.wtune.prover.normalform;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

final class DisjunctionImpl implements Disjunction {
  private final List<Conjunction> components;

  DisjunctionImpl(List<Conjunction> components) {
    this.components = requireNonNull(components);
  }

  public List<Conjunction> conjunctions() {
    return components;
  }

  @Override
  public Disjunction copy() {
    return new DisjunctionImpl(listMap(Conjunction::copy, components));
  }

  @Override
  public void subst(Tuple target, Tuple rep) {
    for (Conjunction component : components) component.subst(target, rep);
  }

  @Override
  public boolean uses(Tuple v) {
    return any(components, it -> it.uses(v));
  }

  @Override
  public UExpr toExpr() {
    return components.stream().map(Conjunction::toExpr).reduce(UExpr::add).orElse(null);
  }

  @Override
  public String toString() {
    return joining(" + ", components);
  }
}
