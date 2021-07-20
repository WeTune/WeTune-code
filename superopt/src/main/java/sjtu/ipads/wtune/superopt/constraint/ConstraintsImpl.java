package sjtu.ipads.wtune.superopt.constraint;

import static sjtu.ipads.wtune.common.utils.FuncUtils.lazyFilter;

import java.util.AbstractList;
import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.common.utils.Congruence;
import sjtu.ipads.wtune.superopt.constraint.Constraint.Kind;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

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
  public Symbol sourceOf(Symbol attrSym) {
    for (Constraint constraint : constraints)
      if (constraint.kind() == Kind.AttrsFrom && constraint.symbols()[0].equals(attrSym))
        return constraint.symbols()[1];
    return null;
  }

  @Override
  public Iterable<Constraint> uniqueKeys() {
    return lazyFilter(constraints, it -> it.kind() == Kind.Unique);
  }

  @Override
  public Iterable<Constraint> foreignKeys() {
    return lazyFilter(constraints, it -> it.kind() == Kind.Reference);
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    return Commons.joining(";", constraints, builder, (it, b) -> it.stringify(naming, b));
  }
}
