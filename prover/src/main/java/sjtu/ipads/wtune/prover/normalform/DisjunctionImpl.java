package sjtu.ipads.wtune.prover.normalform;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

import java.util.List;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

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
    return new DisjunctionImpl(listMap(components, Conjunction::copy));
  }

  @Override
  public boolean uses(Var v) {
    return any(components, it -> it.uses(v));
  }

  @Override
  public void subst(Var target, Var rep) {
    for (Conjunction component : components) component.subst(target, rep);
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
