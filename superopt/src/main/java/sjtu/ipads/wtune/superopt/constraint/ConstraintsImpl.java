package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.common.utils.Congruence;
import sjtu.ipads.wtune.superopt.constraint.Constraint.Kind;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

import java.util.AbstractList;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.lazyFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

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
  public boolean isEq(Symbol s0, Symbol s1) {
    return congruence.isCongruent(s0, s1);
  }

  @Override
  public Set<Symbol> eqClassOf(Symbol symbol) {
    return congruence.eqClassOf(symbol);
  }

  @Override
  public Symbol sourceOf(Symbol attrSym) {
    Symbol cached = null;

    for (Constraint constraint : constraints) {
      final Kind kind = constraint.kind();
      if (constraint.symbols()[0] == attrSym)
        if (kind == Kind.AttrsFrom) return constraint.symbols()[1];
        else if (kind == Kind.AttrsSub) cached = constraint.symbols()[1];
    }

    return cached;
  }

  @Override
  public boolean add(Constraint constraint) {
    constraints.add(constraint);
    if (constraint.kind().isEq())
      congruence.putCongruent(constraint.symbols()[0], constraint.symbols()[1]);
    return true;
  }

  @Override
  public Iterable<Constraint> ofKind(Kind kind) {
    return lazyFilter(constraints, it -> it.kind() == kind);
  }

  @Override
  public StringBuilder canonicalStringify(SymbolNaming naming, StringBuilder builder) {
    final List<String> strings = listMap(constraints, it -> it.canonicalStringify(naming));
    strings.sort(String::compareTo);
    return Commons.joining(";", strings, builder);
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    return Commons.joining(";", constraints, builder, (it, b) -> it.stringify(naming, b));
  }
}
