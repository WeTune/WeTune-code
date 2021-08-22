package sjtu.ipads.wtune.superopt.constraint;

import com.google.common.collect.Multimap;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.prover.logic.LogicCtx;
import sjtu.ipads.wtune.prover.logic.LogicProver;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;
import sjtu.ipads.wtune.superopt.fragment1.Symbols;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.prover.ProverSupport.*;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentSupport.translateAsPlan;
import static sjtu.ipads.wtune.superopt.fragment1.Symbol.Kind.*;

class ConstraintEnumeratorImpl implements ConstraintEnumerator {
  private static final int EQ = 0, CONFLICT = 1, INCOMPLETE = -1, TIMEOUT = 2;
  private final Fragment f0, f1;
  private final ConstraintsIndex constraints;
  private final LogicCtx logicCtx;
  private final boolean[] enabled;
  private int bias;
  private final List<boolean[]> results;
  private final List<Enumerator> enumerators;
  private SymbolNaming naming;

  private long timeout, begin;

  ConstraintEnumeratorImpl(
      Fragment f0, Fragment f1, ConstraintsIndex constraints, LogicCtx logicCtx) {
    this.f0 = f0;
    this.f1 = f1;
    this.constraints = constraints;
    this.logicCtx = logicCtx;

    this.enabled = new boolean[constraints.size()];
    this.results = new LinkedList<>();
    this.enumerators = mkEnumerators();
    this.timeout = Long.MAX_VALUE;

    markMandatory();
    chainEnumerators(enumerators);
  }

  @Override
  public void close() {
    logicCtx.close();
    System.gc();
  }

  @Override
  public void setTimeout(long timeout) {
    if (timeout >= 0) this.timeout = timeout;
  }

  @Override
  public List<List<Constraint>> enumerate() {
    begin = System.currentTimeMillis();

    head(enumerators).enumerate();
    return results();
  }

  @Override
  public List<List<Constraint>> results() {
    return listMap(results, this::mkConstraintSet);
  }

  @Override
  public boolean prove(List<Constraint> selectedConstraints) {
    final boolean[] enabled = new boolean[constraints.size()];
    for (Constraint selected : selectedConstraints) {
      enabled[constraints.indexOf(selected)] = true;
    }
    return prove0(enabled) == EQ;
  }

  @Override
  public boolean prove(int[] selectedConstraints) {
    final boolean[] enabled = new boolean[constraints.size()];
    for (int selected : selectedConstraints) enabled[selected] = true;

    return prove0(enabled) == EQ;
  }

  @Override
  public boolean prove(boolean[] selectedConstraints) {
    return prove0(selectedConstraints) == EQ;
  }

  private int prove0(boolean[] enabled) {
    final Substitution sub = Substitution.mk(f0, f1, mkConstraintSet(enabled));

    final var pair = translateAsPlan(sub, false);
    if (pair == null) return CONFLICT;

    final PlanNode plan0 = disambiguate(pair.getLeft());
    final PlanNode plan1 = disambiguate(pair.getRight());

    final Schema schema = plan0.context().schema();
    final Disjunction d0 = canonizeExpr(normalizeExpr(translateAsUExpr(plan0)), schema);
    final Disjunction d1 = canonizeExpr(normalizeExpr(translateAsUExpr(plan1)), schema);

    final LogicProver prover = mkProver(schema, logicCtx);
    switch (prover.prove(d0, d1)) {
      case EQ:
        return EQ;
      case NEQ:
        return INCOMPLETE;
      case UNKNOWN:
        return CONFLICT;
      default:
        throw new IllegalArgumentException();
    }
  }

  private void markMandatory() {
    final boolean[] bits = constraints.mandatoryBitmap();
    for (int i = 0, bound = bits.length; i < bound; i++) {
      if (bits[i]) enabled[i] = true;
    }
  }

  private List<Enumerator> mkEnumerators() {
    final Symbols symbols0 = constraints.fragment0().symbols();
    final Symbols symbols1 = constraints.fragment1().symbols();
    final List<Symbol> tables0 = symbols0.symbolsOf(TABLE), tables1 = symbols1.symbolsOf(TABLE);
    final List<Symbol> attrs0 = symbols0.symbolsOf(ATTRS), attrs1 = symbols1.symbolsOf(ATTRS);
    final List<Symbol> preds0 = symbols0.symbolsOf(PRED), preds1 = symbols1.symbolsOf(PRED);

    final List<Enumerator> enumerators =
        new ArrayList<>(constraints.size() - constraints.beginIndexOf(AttrsSub) + 5);

    enumerators.add(mkEqRelEnumerator(tables0, tables1));
    enumerators.add(mkEqRelEnumerator(filterNative(attrs0), filterNative(attrs1)));
    enumerators.add(mkEqRelEnumerator(filterDerived(attrs0), filterDerived(attrs1)));
    enumerators.add(mkEqRelEnumerator(preds0, preds1));

    final int attrSubBegin = constraints.beginIndexOf(AttrsSub);
    final int attrSubEnd = constraints.endIndexOf(AttrsSub);
    for (int i = attrSubBegin; i < attrSubEnd; ++i) enumerators.add(new AttrsSubEnumerator(i));

    enumerators.add(new SourceChecker(listJoin(attrs0, attrs1)));

    final int notNullBegin = constraints.beginIndexOf(NotNull);
    final int notNullEnd = constraints.endIndexOf(NotNull);
    for (int i = notNullBegin; i < notNullEnd; ++i) enumerators.add(new SourceAwareEnumerator(i));

    final int uniqueBegin = constraints.beginIndexOf(Unique);
    final int uniqueEnd = constraints.endIndexOf(Unique);
    for (int i = uniqueBegin; i < uniqueEnd; ++i) enumerators.add(new SourceAwareEnumerator(i));

    final int refBegin = constraints.beginIndexOf(Reference);
    final int refEnd = constraints.endIndexOf(Reference);
    for (int i = refBegin; i < refEnd; ++i) enumerators.add(new ReferenceEnumerator(i));

    enumerators.add(new Recorder());
    enumerators.add(new Timeout());
    //    enumerators.add(new Dummy());
    enumerators.add(new Proof());

    return enumerators;
  }

  private Enumerator mkEqRelEnumerator(List<Symbol> symbols0, List<Symbol> symbols1) {
    return new EqRelEnumerator(symbols0, symbols1);
  }

  private List<Symbol> filterDerived(List<Symbol> attrs) {
    final Multimap<Symbol, Symbol> sources = constraints.attrSources();
    return listFilter(attrs, it -> any(sources.get(it), src -> src.kind() == ATTRS));
  }

  private List<Symbol> filterNative(List<Symbol> attrs) {
    final Multimap<Symbol, Symbol> sources = constraints.attrSources();
    return listFilter(attrs, it -> any(sources.get(it), src -> src.kind() == TABLE));
  }

  private void chainEnumerators(List<Enumerator> enumerators) {
    for (int i = 1, bound = enumerators.size(); i < bound; i++)
      enumerators.get(i - 1).setNext(enumerators.get(i));
  }

  private boolean isEq(Symbol x, Symbol y) {
    return x == y || (x.kind() == y.kind() && enabled[constraints.indexOfEq(x, y)]);
  }

  private Symbol sourceOf(Symbol x) {
    assert x.kind() == ATTRS;

    final int attrSubBegin = constraints.beginIndexOf(AttrsSub);
    final int attrSubEnd = constraints.endIndexOf(AttrsSub);

    for (int i = attrSubBegin; i < attrSubEnd; ++i) {
      final Constraint attrSub = constraints.get(i);
      if (enabled[i] && attrSub.symbols()[0] == x) return attrSub.symbols()[1];
    }

    return null;
  }

  private List<Constraint> mkConstraintSet(boolean[] enabled) {
    final List<Constraint> set = new ArrayList<>(constraints.size());
    for (int i = 0, bound = constraints.size(); i < bound; i++)
      if (enabled[i]) set.add(constraints.get(i));
    return set;
  }

  private SymbolNaming naming() {
    if (naming == null) {
      naming = SymbolNaming.mk();
      naming.name(f0.symbols());
      naming.name(f1.symbols());
    }
    return naming;
  }

  private Substitution mkSubstitution(boolean[] enabled) {
    return Substitution.mk(f0, f1, mkConstraintSet(enabled));
  }

  private abstract static class Enumerator {
    protected Enumerator next;

    public void setNext(Enumerator next) {
      this.next = next;
    }

    abstract int enumerate();
  }

  private class EqRelEnumerator extends Enumerator {
    private final int segIndex;
    private final List<Symbol> symbols;
    private final Partitioner partitioner;

    private EqRelEnumerator(List<Symbol> lhsSymbols, List<Symbol> rhsSymbols) {
      this.segIndex = lhsSymbols.size();
      this.symbols = listJoin(lhsSymbols, rhsSymbols);
      this.partitioner = new Partitioner((byte) symbols.size());
    }

    @Override
    public int enumerate() {
      if (symbols.isEmpty()) return next.enumerate();

      partitioner.reset();
      final TIntList buffer = new TIntArrayList((symbols.size() * (symbols.size() - 1)) >> 1);

      do {
        final int originalBias = bias;
        final byte[][] partitions = partitioner.partition();

        if (!checkComplete(partitions)) continue;

        for (byte[] partition : partitions)
          for (int i = 0, bound = partition.length; i < bound - 1; ++i)
            for (int j = i + 1; j < bound; ++j) {
              final int index =
                  constraints.indexOfEq(symbols.get(partition[i]), symbols.get(partition[j]));
              enabled[index] = true;
              buffer.add(index);
            }

        // We don't care about the other conditions.
        if (next.enumerate() == TIMEOUT) return TIMEOUT;

        // Reset before next iteration.
        for (int i = 0, bound = buffer.size(); i < bound; ++i) enabled[buffer.get(i)] = false;
        buffer.clear();
        bias = originalBias;

      } while (partitioner.forward());

      return 0; // Doesn't matter.
    }

    private boolean checkComplete(byte[][] partitions) {
      // Check whether all the symbols from one of the sides are connected with another symbol from
      // another side.
      final boolean[] connected = new boolean[symbols.size()];
      for (byte[] partition : partitions) {
        final byte min = partition[0], max = partition[partition.length - 1];
        if (!(min < segIndex && segIndex <= max)) continue;
        for (byte b : partition) connected[b] = true;
      }

      for (int i = 0, bound = symbols.size(); i < bound; ++i) {
        if (!connected[i]) {
          // `bias` indicates which side is not fully connected.
          if (bias == 0) bias = i < segIndex ? -1 : 1;
          // Both sides are not fully connected.
          else if (bias < 0 && i >= segIndex) return false;
          else if (bias > 0 && i < segIndex) return false;
        }
      }

      return true;
    }
  }

  private abstract class PruningEnumerator extends Enumerator {
    protected final int index;

    protected PruningEnumerator(int index) {
      this.index = index;
    }

    @Override
    int enumerate() {
      final boolean original = enabled[index];
      final boolean canRelax = !original && !isImplied();
      int answer = CONFLICT;

      enabled[index] = true;
      if (checkNoConflict()) answer = next.enumerate();
      if (!canRelax) {
        enabled[index] = original;
        return answer;
      }

      enabled[index] = false;
      if (answer == INCOMPLETE || answer == TIMEOUT) return answer;

      final int answer1 = next.enumerate();
      return answer == EQ ? EQ : answer1;
    }

    protected boolean isImplied() {
      final Constraint me = constraints.get(index);
      final Constraint.Kind kind = me.kind();

      final int start = constraints.beginIndexOf(kind);
      final int end = constraints.endIndexOf(kind);

      for (int i = start; i < end; ++i) {
        if (i == index || !enabled[i]) continue;
        if (canImply(constraints.get(i), me)) return true;
      }

      return false;
    }

    protected abstract boolean canImply(Constraint pre, Constraint post);

    protected abstract boolean checkNoConflict();
  }

  private class AttrsSubEnumerator extends PruningEnumerator {
    private AttrsSubEnumerator(int index) {
      super(index);
    }

    @Override
    protected boolean checkNoConflict() {
      final Constraint me = constraints.get(index);
      final Symbol attr = me.symbols()[0];
      final Symbol source = me.symbols()[1];
      final int begin = constraints.beginIndexOf(AttrsSub);
      final int end = constraints.endIndexOf(AttrsSub);

      for (int i = begin; i < end; ++i) {
        final Constraint c = constraints.get(i);
        if (enabled[i]) {
          final Symbol thatAttr = c.symbols()[0];
          final Symbol thatSource = c.symbols()[1];

          if ((attr == thatAttr && source != thatSource)
              || isEq(attr, thatAttr) && !isEq(source, thatSource)) {
            return false;
          }
        } else if (i < index && canImply(c, me)) {
          return false; // We should be suppressed.
        }
      }

      return true;
    }

    @Override
    protected boolean canImply(Constraint pre, Constraint post) {
      final Symbol[] syms0 = pre.symbols();
      final Symbol[] syms1 = post.symbols();
      if (!isEq(syms0[0], syms1[0])) return false;

      final Symbol src0 = syms0[1], src1 = syms1[1];
      if (src0 == src1) return true;
      if (src0.ctx() == src1.ctx() || !isEq(src0, src1)) return false;
      return countEqsOf(src0) == 1;
    }

    private int countEqsOf(Symbol sym) {
      int count = 0;
      for (Symbol other : f0.symbols().symbolsOf(sym.kind())) if (isEq(sym, other)) ++count;
      for (Symbol other : f1.symbols().symbolsOf(sym.kind())) if (isEq(sym, other)) ++count;
      return count;
    }
  }

  private class SourceChecker extends Enumerator {
    private final Collection<Symbol> symbols;

    private SourceChecker(Collection<Symbol> attrSyms) {
      symbols = listFilter(attrSyms, it -> countRelatedAttrSubs(it) > 1);
    }

    private int countRelatedAttrSubs(Symbol sym) {
      final int begin = constraints.beginIndexOf(AttrsSub);
      final int end = constraints.endIndexOf(AttrsSub);
      int count = 0;
      for (Constraint constraint : constraints.subList(begin, end))
        for (Symbol symbol : constraint.symbols())
          if (symbol == sym) {
            ++count;
            break;
          }
      return count;
    }

    @Override
    int enumerate() {
      for (Symbol symbol : symbols) {
        if (sourceOf(symbol) == null) return CONFLICT;
      }
      return next.enumerate();
    }
  }

  private class SourceAwareEnumerator extends PruningEnumerator {
    private final Symbol attr, source;

    protected SourceAwareEnumerator(int index) {
      super(index);
      final Constraint constraint = constraints.get(index);
      this.source = constraint.symbols()[0];
      this.attr = constraint.symbols()[1];
    }

    @Override
    protected boolean checkNoConflict() {
      if (source != sourceOf(attr)) return false;
      final Constraint me = constraints.get(index);
      final int begin = constraints.beginIndexOf(me.kind());
      for (int i = begin; i < index; ++i)
        if (!enabled[i] && canImply(constraints.get(i), me)) {
          return false; // We are suppressed.
        }
      return true;
    }

    @Override
    protected boolean canImply(Constraint pre, Constraint post) {
      final Symbol[] syms0 = pre.symbols();
      final Symbol[] syms1 = post.symbols();
      if (!isEq(syms0[1], syms1[1])) return false;

      return isEq(syms0[0], syms1[0]) && sourceOf(syms1[1]) == syms1[0];
    }
  }

  private class ReferenceEnumerator extends PruningEnumerator {
    private final Symbol attr0, source0, attr1, source1;

    protected ReferenceEnumerator(int index) {
      super(index);
      final Constraint constraint = constraints.get(index);
      this.source0 = constraint.symbols()[0];
      this.attr0 = constraint.symbols()[1];
      this.source1 = constraint.symbols()[2];
      this.attr1 = constraint.symbols()[3];
    }

    @Override
    protected boolean checkNoConflict() {
      if (!(source0 == sourceOf(attr0) && source1 == sourceOf(attr1))) return false;

      final int begin = constraints.beginIndexOf(Reference);
      final Constraint me = constraints.get(index);
      for (int i = begin; i < index; ++i)
        if (!enabled[i] && canImply(constraints.get(i), me)) {
          return false; // We are suppressed.
        }

      return true;
    }

    @Override
    protected boolean canImply(Constraint pre, Constraint post) {
      final Symbol[] syms0 = pre.symbols();
      final Symbol[] syms1 = post.symbols();
      final Symbol attrs00 = syms0[1], attrs01 = syms0[3], attrs10 = syms1[1], attrs11 = syms1[3];
      if (!isEq(attrs00, attrs10) || !isEq(attrs01, attrs11)) return false;

      final Symbol src00 = syms0[0], src01 = syms0[2], src10 = syms1[0], src11 = syms1[2];
      return isEq(src00, src10)
          && isEq(src01, src11)
          && sourceOf(attrs10) == src10
          && sourceOf(attrs11) == src11;
    }
  }

  private class Timeout extends Enumerator {
    @Override
    int enumerate() {
      if (System.currentTimeMillis() - begin > timeout) return TIMEOUT;
      return next.enumerate();
    }
  }

  private class Recorder extends Enumerator {
    private int i = 0;

    @Override
    int enumerate() {
      //      System.out.print(i++);
      //      System.out.print(" ");
      final int answer = next.enumerate();
      //      System.out.println("=> " + answer);
      if (answer == EQ) {
        final boolean[] result = Arrays.copyOf(enabled, enabled.length);
        results.removeIf(it -> isWeakerThan(result, it));
        results.add(result);
      }
      return answer;
    }

    private boolean isWeakerThan(boolean[] res0, boolean[] res1) {
      for (int i = 0, bound = res0.length; i < bound; ++i) if (res0[i] && !res1[i]) return false;
      return true;
    }
  }

  private class Proof extends Enumerator {
    @Override
    int enumerate() {
      return prove0(enabled);
    }
  }

  private class Dummy extends Enumerator {
    @Override
    int enumerate() {
      System.out.println(Arrays.toString(enabled));
      return CONFLICT;
    }
  }
}
