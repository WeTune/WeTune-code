package sjtu.ipads.wtune.superopt.constraint;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment1.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterables.concat;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.common.utils.FuncUtils.locate;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.fragment1.Symbol.Kind.*;

// The class maintains a collection of constraint and corresponding metadata.
class ConstraintsIndex extends AbstractList<Constraint> {
  private final Fragment f0, f1;
  private final Symbols symbols0, symbols1;
  private final List<Constraint> constraints;
  // Possible sources of attributes. Key: Attrs symbols. Value: Attrs/Table symbols.
  private final Multimap<Symbol, Symbol> sources;
  private final boolean[] mandatory; // Indicates whether a constraint must be present.
  private final boolean[] useless; // If whether a constraint is meaningless.
  // The constraints are organized by the kind. i.e., [TablEqs, AttrsEqs, PredEqs, AttrSubs,...]
  // `segBase` is the beginning index of each segment.
  private final int[] segBases;
  private final int[] symCounts; // The count of symbols of each kinds.
  // The ordinal of a symbol. Used to quickly calculates the index of XxEq constraint.
  private final TObjectIntMap<Symbol> symbolsIndex;

  ConstraintsIndex(
      Fragment f0, Fragment f1, List<Constraint> constraints, Multimap<Symbol, Symbol> sources) {
    this.f0 = f0;
    this.f1 = f1;
    this.symbols0 = f0.symbols();
    this.symbols1 = f1.symbols();
    this.constraints = constraints;
    this.sources = sources;
    this.mandatory = new boolean[constraints.size()];
    this.useless = new boolean[constraints.size()];
    this.segBases = new int[Constraint.Kind.values().length];
    this.symCounts = new int[3];
    this.symbolsIndex = new TObjectIntHashMap<>();

    calcSegments();
    calcMandatory();
    calcUseless();
    buildSymbolsIndex();
  }

  static ConstraintsIndex mk(Fragment f0, Fragment f1) {
    final Symbols symbols0 = f0.symbols();
    final Symbols symbols1 = f1.symbols();

    final List<Constraint> constraints = new ArrayList<>(symbols0.size() * symbols1.size());

    final List<Symbol> tables0 = symbols0.symbolsOf(TABLE), tables1 = symbols1.symbolsOf(TABLE);
    final List<Symbol> attrs0 = symbols0.symbolsOf(ATTRS), attrs1 = symbols1.symbolsOf(ATTRS);
    final List<Symbol> preds0 = symbols0.symbolsOf(PRED), preds1 = symbols1.symbolsOf(PRED);
    final Multimap<Symbol, Symbol> sources = analyzeSources(f0);
    sources.putAll(analyzeSources(f1));

    mkEqRel(TableEq, listJoin(tables0, tables1), constraints);
    mkEqRel(AttrsEq, listJoin(attrs0, attrs1), constraints);
    mkEqRel(PredicateEq, listJoin(preds0, preds1), constraints);
    mkAttrsSub(sources, constraints);
    mkUnique(sources, constraints);
    mkNotNull(sources, constraints);
    mkReferences(f0, sources, constraints);
    mkReferences(f1, sources, constraints);

    return new ConstraintsIndex(f0, f1, constraints, sources);
  }

  private static void mkEqRel(Constraint.Kind kind, List<Symbol> symbols, List<Constraint> buffer) {
    for (int i = 0, bound = symbols.size(); i < bound - 1; i++)
      for (int j = i + 1; j < bound; j++) {
        buffer.add(Constraint.mk(kind, symbols.get(i), symbols.get(j)));
      }
  }

  private static void mkAttrsSub(Multimap<Symbol, Symbol> sources, List<Constraint> buffer) {
    for (var entry : sources.entries()) {
      buffer.add(Constraint.mk(AttrsSub, entry.getKey(), entry.getValue()));
    }
  }

  private static void mkUnique(Multimap<Symbol, Symbol> sources, List<Constraint> buffer) {
    for (var entry : sources.entries())
      if (entry.getValue().kind() == TABLE) {
        buffer.add(Constraint.mk(Unique, entry.getValue(), entry.getKey()));
      }
  }

  private static void mkNotNull(Multimap<Symbol, Symbol> sources, List<Constraint> buffer) {
    for (var entry : sources.entries())
      if (entry.getValue().kind() == TABLE) {
        buffer.add(Constraint.mk(NotNull, entry.getValue(), entry.getKey()));
      }
  }

  private static void mkReferences(
      Fragment fragment, Multimap<Symbol, Symbol> sources, List<Constraint> buffer) {
    fragment.acceptVisitor(OpVisitor.traverse(it -> mkReferences0(it, sources, buffer)));
  }

  private static void mkReferences0(
      Op op, Multimap<Symbol, Symbol> sources, List<Constraint> buffer) {
    // We only consider the Attrs as join key
    if (!op.kind().isJoin()) return;

    final Symbols symbols = op.fragment().symbols();
    final Join join = (Join) op;
    final Symbol lhsAttrs = join.lhsAttrs(), rhsAttrs = join.rhsAttrs();
    final Collection<Symbol> lhsSources = sources.get(lhsAttrs), rhsSources = sources.get(rhsAttrs);

    for (Symbol lhsSource : lhsSources)
      for (Symbol rhsSource : rhsSources)
        mkReferences0(lhsSource, lhsAttrs, rhsSource, rhsAttrs, symbols, sources, buffer);
  }

  private static void mkReferences0(
      Symbol lhsSource,
      Symbol lhsAttr,
      Symbol rhsSource,
      Symbol rhsAttr,
      Symbols symbols,
      Multimap<Symbol, Symbol> sources,
      List<Constraint> buffer) {
    if (lhsSource.kind() == ATTRS) {
      final Symbol referredAttrs = ((Proj) symbols.ownerOf(lhsSource)).inAttrs();
      for (Symbol actualSource : sources.get(referredAttrs))
        mkReferences0(actualSource, referredAttrs, rhsSource, rhsAttr, symbols, sources, buffer);
      return;
    }

    if (rhsSource.kind() == ATTRS) {
      final Symbol referredAttrs = ((Proj) symbols.ownerOf(rhsSource)).inAttrs();
      for (Symbol actualSource : sources.get(referredAttrs))
        mkReferences0(lhsSource, lhsAttr, actualSource, referredAttrs, symbols, sources, buffer);
      return;
    }

    buffer.add(Constraint.mk(Reference, lhsSource, lhsAttr, rhsSource, rhsAttr));
  }

  private void buildSymbolsIndex() {
    symCounts[0] = buildSymbolsIndex0(concat(symbols0.symbolsOf(TABLE), symbols1.symbolsOf(TABLE)));
    symCounts[1] = buildSymbolsIndex0(concat(symbols0.symbolsOf(ATTRS), symbols1.symbolsOf(ATTRS)));
    symCounts[2] = buildSymbolsIndex0(concat(symbols0.symbolsOf(PRED), symbols1.symbolsOf(PRED)));
  }

  private int buildSymbolsIndex0(Iterable<Symbol> symbols) {
    int i = 0;
    for (Symbol symbol : symbols) symbolsIndex.put(symbol, i++);
    return i;
  }

  private void calcMandatory() {
    final int begin = segBases[AttrsSub.ordinal()], end = segBases[AttrsSub.ordinal() + 1];
    for (int i = begin; i < end; i++)
      if (sources.get(constraints.get(i).symbols()[0]).size() <= 1) {
        mandatory[i] = true;
      }
  }

  private void calcUseless() {
    final int begin = segBases[AttrsEq.ordinal()], end = segBases[AttrsEq.ordinal() + 1];
    for (int i = begin; i < end; i++) {
      final Constraint c = constraints.get(i);
      final Symbol attrs0 = c.symbols()[0], attrs1 = c.symbols()[1];
      final Collection<Symbol> sources0 = sources.get(attrs0), sources1 = sources.get(attrs1);

      final boolean hasTableSource0 = any(sources0, it -> it.kind() == TABLE);
      final boolean hasTableSource1 = any(sources1, it -> it.kind() == TABLE);
      final boolean hasSubquerySource0 = any(sources0, it -> it.kind() == ATTRS);
      final boolean hasSubquerySource1 = any(sources1, it -> it.kind() == ATTRS);

      if ((!hasSubquerySource0 && !hasTableSource1) || (!hasTableSource0 && !hasSubquerySource1)) {
        useless[i] = true;
      }
    }
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

    for (int i = 0; i < numKinds; i++) {
      if (segBases[i] == -1) segBases[i] = segBases[i + 1];
    }
  }

  public Fragment fragment0() {
    return f0;
  }

  public Fragment fragment1() {
    return f1;
  }

  public boolean[] mandatoryBitmap() {
    return mandatory;
  }

  public Multimap<Symbol, Symbol> attrSources() {
    return sources;
  }

  public int beginIndexOf(Constraint.Kind kind) {
    return segBases[kind.ordinal()];
  }

  public int endIndexOf(Constraint.Kind kind) {
    return segBases[kind.ordinal() + 1];
  }

  public int indexOfEq(Symbol s0, Symbol s1) {
    assert s0.kind() == s1.kind();
    final Symbol.Kind kind = s0.kind();
    final int base = segBases[kind.ordinal()];
    final int symCount = symCounts[kind.ordinal()];
    final int i = symbolsIndex.get(s0), j = symbolsIndex.get(s1);
    final int x = Math.min(i, j), y = Math.max(i, j);
    return base + ((((symCount << 1) - x - 1) * x) >> 1) + y - x - 1;
  }

  @Override
  public Constraint get(int index) {
    return constraints.get(index);
  }

  @Override
  public int size() {
    return constraints.size();
  }

  private static Multimap<Symbol, Symbol> analyzeSources(Fragment fragment) {
    final AttrSourceAnalyzer analyzer = new AttrSourceAnalyzer(fragment.symbols());
    analyzer.analyze0(fragment.root());
    return analyzer.sources;
  }

  private static class AttrSourceAnalyzer {
    private final Symbols symbols;
    private final Multimap<Symbol, Symbol> sources;

    private AttrSourceAnalyzer(Symbols symbols) {
      this.symbols = symbols;
      this.sources = LinkedListMultimap.create(16);
    }

    List<Symbol> analyze0(Op op) {
      final OperatorType type = op.kind();
      final List<Symbol> lhs = type.numPredecessors() >= 1 ? analyze0(op.predecessors()[0]) : null;
      final List<Symbol> rhs = type.numPredecessors() >= 2 ? analyze0(op.predecessors()[1]) : null;

      switch (type) {
        case INPUT:
          return singletonList(symbols.symbolAt(op, TABLE, 0));
        case LEFT_JOIN:
        case INNER_JOIN:
          {
            sources.putAll(symbols.symbolAt(op, ATTRS, 0), lhs);
            sources.putAll(symbols.symbolAt(op, ATTRS, 1), rhs);
            return listJoin(lhs, rhs);
          }
        case SIMPLE_FILTER:
        case IN_SUB_FILTER:
          {
            sources.putAll(symbols.symbolAt(op, ATTRS, 0), lhs);
            return lhs;
          }
        case PROJ:
          {
            final Symbol attrs0 = symbols.symbolAt(op, ATTRS, 0);
            final Symbol attrs1 = symbols.symbolAt(op, ATTRS, 1);
            sources.putAll(attrs0, lhs);
            return singletonList(attrs1);
          }
        default:
          throw new IllegalArgumentException();
      }
    }
  }
}
