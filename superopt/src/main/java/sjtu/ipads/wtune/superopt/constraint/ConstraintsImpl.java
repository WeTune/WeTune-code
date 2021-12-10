package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.common.utils.NaturalCongruence;
import sjtu.ipads.wtune.superopt.constraint.Constraint.Kind;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;
import sjtu.ipads.wtune.superopt.fragment.Symbols;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.locate;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.common.utils.IterableSupport.lazyFilter;

class ConstraintsImpl extends AbstractList<Constraint> implements Constraints {
  private final List<Constraint> constraints;
  private final NaturalCongruence<Symbol> congruence;
  private final Map<Symbol, Symbol> instantiationSource;
  private final int[] segBases;

  ConstraintsImpl(
      List<Constraint> constraints,
      NaturalCongruence<Symbol> congruence,
      Map<Symbol, Symbol> instantiationSource) {
    this.constraints = constraints;
    this.congruence = congruence;
    this.instantiationSource = instantiationSource;

    this.segBases = new int[Constraint.Kind.values().length];
    calcSegments();
  }

  static Constraints mk(Symbols srcSyms, List<Constraint> constraints) {
    // This congruence contains only the symbols at the source side
    final NaturalCongruence<Symbol> eqSymbols = NaturalCongruence.mk();
    for (Constraint c : lazyFilter(constraints, it -> it.kind().isEq())) {
      if (any(asList(c.symbols()), it -> it.ctx() != srcSyms)) continue;
      eqSymbols.putCongruent(c.symbols()[0], c.symbols()[1]);
    }

    // This map contains <tgtSym -> srcSym>, which means the `tgtSym` is instantiated from `srcSym`
    final Map<Symbol, Symbol> instantiationSource = new IdentityHashMap<>();
    for (Constraint c : lazyFilter(constraints, it -> it.kind().isEq())) {
      Symbol sym0 = c.symbols()[0], sym1 = c.symbols()[1];
      if (sym1.ctx() == srcSyms && sym0.ctx() != srcSyms) {
        Symbol tmp = sym0;
        sym0 = sym1;
        sym1 = tmp;
      }

      // add this after migration completion:
      //      assert sym0.ctx() == srcSyms && sym1.ctx() != srcSyms;

      instantiationSource.put(sym1, sym0);
    }

    constraints.sort(comparing(Constraint::kind));

    return new ConstraintsImpl(constraints, eqSymbols, instantiationSource);
  }

  private void calcSegments() {
    final int numKinds = Constraint.Kind.values().length - 1, bound = constraints.size();
    int begin = 0;

    for (int i = 0; i < numKinds; i++) {
      final Constraint.Kind kind = Constraint.Kind.values()[i];
      final int seg = begin + locate(constraints.subList(begin, bound), it -> it.kind() == kind);
      if (seg >= begin) begin = segBases[i] = seg;
      else segBases[i] = -1;
    }
    segBases[segBases.length - 1] = bound;

    for (int i = numKinds - 1; i >= 0; i--) {
      if (segBases[i] == -1) segBases[i] = segBases[i + 1];
    }
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
    // AttrsFrom takes priority.
    for (Constraint constraint : ofKind(Kind.AttrsFrom)) {
      if (constraint.symbols()[0] == attrSym) return constraint.symbols()[1];
    }
    for (Constraint constraint : ofKind(Kind.AttrsSub)) {
      if (constraint.symbols()[0] == attrSym) return constraint.symbols()[1];
    }
    return null;
  }

  @Override
  public boolean add(Constraint constraint) {
    constraints.add(constraint);
    if (constraint.kind().isEq())
      congruence.putCongruent(constraint.symbols()[0], constraint.symbols()[1]);
    return true;
  }

  @Override
  public List<Constraint> ofKind(Kind kind) {
    return constraints.subList(beginIndexOf(kind), endIndexOf(kind));
  }

  @Override
  public StringBuilder canonicalStringify(SymbolNaming naming, StringBuilder builder) {
    final List<String> strings = listMap(constraints, it -> it.canonicalStringify(naming));
    strings.sort(String::compareTo);
    return Commons.joining(";", strings, builder);
  }

  @Override
  public NaturalCongruence<Symbol> eqSymbols() {
    return congruence;
  }

  @Override
  public Symbol instantiationOf(Symbol tgtSym) {
    return instantiationSource.get(tgtSym);
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    return Commons.joining(";", constraints, builder, (it, b) -> it.stringify(naming, b));
  }

  private int beginIndexOf(Constraint.Kind kind) {
    return segBases[kind.ordinal()];
  }

  private int endIndexOf(Constraint.Kind kind) {
    if (kind == Kind.AttrsFrom) return constraints.size();
    return segBases[kind.ordinal() + 1];
  }
}
