package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.common.utils.Congruence;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

import java.util.AbstractList;
import java.util.List;
import java.util.Set;

class ConstraintsImpl extends AbstractList<Constraint> implements Constraints {
  private final List<Constraint> constraints;
  private final Congruence<Symbol> congruence;

  ConstraintsImpl(List<Constraint> constraints, Congruence<Symbol> congruence) {
    this.constraints = constraints;
    this.congruence = congruence;
  }

  static Constraints mk(List<Constraint> constraints) {
    final Congruence<Symbol> congruence = Congruence.mk();
    for (Constraint constraint : constraints)
      if (constraint.kind().isEq())
        congruence.putCongruent(constraint.symbols()[0], constraint.symbols()[1]);

    return new ConstraintsImpl(constraints, congruence);
  }

  @Override
  public Constraint get(int index) {
    return constraints.get(index);
  }

  @Override
  public int size() {
    return constraints.size();
  }

  @Override
  public Set<Symbol> eqClassOf(Symbol symbol) {
    return congruence.eqClassOf(symbol);
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    return Commons.joining(";", constraints, builder, (it, b) -> it.stringify(naming, b));
  }
}
