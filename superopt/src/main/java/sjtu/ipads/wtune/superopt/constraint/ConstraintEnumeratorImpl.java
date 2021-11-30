package sjtu.ipads.wtune.superopt.constraint;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.LeveledException;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.prover.logic.LogicCtx;
import sjtu.ipads.wtune.prover.logic.LogicProver;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;
import sjtu.ipads.wtune.superopt.fragment.Symbols;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.ProverSupport.*;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

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
  private final Map<Symbol.Kind, byte[][]> currPartitions;

  private long timeout, begin;

  // Statistics
  private int proverInvokeTimes;
  // debug
  private int partitionCount;

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

    this.currPartitions = new EnumMap<Symbol.Kind, byte[][]>(Symbol.Kind.class);
    this.currPartitions.put(TABLE, null);
    this.currPartitions.put(ATTRS, null);
    this.currPartitions.put(PRED, null);

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
    System.out.println("Invoke prover times: " + proverInvokeTimes);
    System.out.println("Partition times: " + partitionCount);
    System.out.println("Rule nums: " + results.size());
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

    final var pair = translateAsPlan(sub, false, true);
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
    for (int i = 0, bound = bits.length; i < bound; i++) if (bits[i]) enabled[i] = true;
  }

  private List<Enumerator> mkEnumerators() {
    final Symbols symbols0 = constraints.fragment0().symbols();
    final Symbols symbols1 = constraints.fragment1().symbols();
    final List<Symbol> tables0 = symbols0.symbolsOf(TABLE), tables1 = symbols1.symbolsOf(TABLE);
    final List<Symbol> attrs0 = symbols0.symbolsOf(ATTRS), attrs1 = symbols1.symbolsOf(ATTRS);
    final List<Symbol> preds0 = symbols0.symbolsOf(PRED), preds1 = symbols1.symbolsOf(PRED);

    final List<Enumerator> enumerators =
        new ArrayList<>(
            constraints.size() - constraints.beginIndexOf(AttrsSub) + symbols1.size() + 12);

    // Set precondition stage
    enumerators.add(new EqRelEnumerator(tables0));
    enumerators.add(new EqRelEnumerator(attrs0));
    enumerators.add(new EqRelEnumerator(preds0));
    enumerators.add(new Timeout());

    // Instantiate stage
    for (int i = 0; i < tables1.size(); ++i)
      enumerators.add(new InstantiateEnumerator(tables1.get(i), tables0));
    for (int i = 0; i < attrs1.size(); ++i)
      enumerators.add(new InstantiateEnumerator(attrs1.get(i), attrs0));
    for (int i = 0; i < preds1.size(); ++i)
      enumerators.add(new InstantiateEnumerator(preds1.get(i), preds0));
    enumerators.add(new Timeout());

    final int attrSubBegin = constraints.beginIndexOf(AttrsSub);
    final int attrSubEnd = constraints.endIndexOf(AttrsSub);
    for (int i = attrSubBegin; i < attrSubEnd; ++i) enumerators.add(new AttrsSubEnumerator(i));
    enumerators.add(new Timeout());

    enumerators.add(new SourceChecker(attrs0));
    enumerators.add(new InstanceSourceChecker(symbols1));

    final int notNullBegin = constraints.beginIndexOf(NotNull);
    final int notNullEnd = constraints.endIndexOf(NotNull);
    for (int i = notNullBegin; i < notNullEnd; ++i) enumerators.add(new SourceAwareEnumerator(i));

    final int uniqueBegin = constraints.beginIndexOf(Unique);
    final int uniqueEnd = constraints.endIndexOf(Unique);
    for (int i = uniqueBegin; i < uniqueEnd; ++i) enumerators.add(new SourceAwareEnumerator(i));

    final int refBegin = constraints.beginIndexOf(Reference);
    final int refEnd = constraints.endIndexOf(Reference);
    for (int i = refBegin; i < refEnd; ++i) enumerators.add(new ReferenceEnumerator(i));

    enumerators.add(new Timeout());
    enumerators.add(new Recorder());
    enumerators.add(new Proof());

    return enumerators;
  }

  private void chainEnumerators(List<Enumerator> enumerators) {
    for (int i = 1, bound = enumerators.size(); i < bound; i++)
      enumerators.get(i - 1).setNext(enumerators.get(i));
  }

  private boolean isEq(Symbol x, Symbol y) {
    return x == y || (x.kind() == y.kind() && enabled[constraints.indexOfEq(x, y)]);
  }

  private boolean indirectEq(Symbol x, Symbol y) {
    if (isEq(x, y)) return true;
    if (x.kind() == TABLE) return false;

    return indirectEq(directSourceOf(x), y);
  }

  private Symbol directSourceOf(Symbol x) {
    assert x.kind() == ATTRS;

    final int attrSubBegin = constraints.beginIndexOf(AttrsSub);
    final int attrSubEnd = constraints.endIndexOf(AttrsSub);

    for (int i = attrSubBegin; i < attrSubEnd; ++i) {
      final Constraint attrSub = constraints.get(i);
      if (enabled[i] && attrSub.symbols()[0] == x) return attrSub.symbols()[1];
    }

    return null;
  }

  private Symbol fastDirectSourceOf(Symbol x, int indexHint) {
    assert x.kind() == ATTRS;
    final int attrsSubBegin = constraints.beginIndexOf(AttrsSub);
    for (int i = indexHint; i >= attrsSubBegin; i--) {
      if (enabled[i]) {
        final Constraint constraint = constraints.get(i);
        if (constraint.symbols()[0] != x) return null;
        else return constraint.symbols()[1];
      }
    }

    return null;
  }

  private Symbol indirectSourceOf(Symbol x) {
    assert x.kind() == ATTRS;
    assert x.ctx() == f1.symbols();

    Symbol eqSym = null, eqSource = null;
    for (Symbol symbol : f0.symbols().symbolsOf(ATTRS))
      if (isEq(x, symbol)) {
        eqSym = symbol;
        break;
      }

    if (eqSym == null) return null;
    if ((eqSource = directSourceOf(eqSym)) == null) return null;

    Collection<Symbol> candidateSources = constraints.getAttrSources(x);
    for (Symbol symbol :
        ListSupport.join(f1.symbols().symbolsOf(TABLE), f1.symbols().symbolsOf(ATTRS)))
      if (indirectEq(eqSource, symbol) && candidateSources.contains(symbol)) return symbol;

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

  private abstract class PruningEnumerator extends Enumerator {
    protected final int index;

    protected PruningEnumerator(int index) {
      this.index = index;
    }

    @Override
    int enumerate() {
      //      if (constraints.get(index).kind() == Unique && f1.hasDedup())
      //        return relaxBackward();

      return relaxForward();
    }

    private int relaxForward() {
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

    private int relaxBackward() {
      final boolean original = enabled[index];
      final boolean canRelax = !original && !isImplied(); // mandatory or implied(remain true)
      int answer = CONFLICT;

      if (canRelax) {
        enabled[index] = false;
        answer = next.enumerate();
        if (answer == EQ) return answer;
      }

      if (answer == TIMEOUT) return answer;

      enabled[index] = true;
      if (checkNoConflict()) answer = next.enumerate();
      enabled[index] = original;

      return answer;
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

  private class EqRelEnumerator extends Enumerator {
    private final int segIndex;
    private final List<Symbol> symbols;
    private final Partitioner partitioner;

    private EqRelEnumerator(List<Symbol> symbols) {
      this.segIndex = symbols.size();
      this.symbols = symbols;
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
        currPartitions.put(symbols.get(0).kind(), partitions);

        if (System.currentTimeMillis() - begin > timeout) return TIMEOUT;

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
      for (Symbol symbol : symbols) if (directSourceOf(symbol) == null) return CONFLICT;
      return next.enumerate();
    }
  }

  private class AttrsSubEnumerator extends PruningEnumerator {
    private final Symbol attrs, source;

    private AttrsSubEnumerator(int index) {
      super(index);
      final Constraint constraint = constraints.get(index);
      this.attrs = constraint.symbols()[0];
      this.source = constraint.symbols()[1];
    }

    @Override
    protected boolean checkNoConflict() {
      return checkSingleSource() // Check no other AttrsSub of this attrs is enabled.
          && checkCompatible(); // Check no other equal attrs has incompatible source.
    }

    @Override
    protected boolean canImply(Constraint pre, Constraint post) {
      // AttrsSub can never be implied - even isEq(pre.attrs,post.attrs) && isEq(pre.src,post.src)
      return false;
    }

    private boolean checkSingleSource() {
      return fastDirectSourceOf(attrs, index - 1) == null;
    }

    private boolean checkCompatible() {
      final int bound = constraints.beginIndexOf(AttrsSub);
      for (int i = index - 1; i >= bound; i--) {
        if (!enabled[i]) continue;

        final Constraint that = constraints.get(i);
        final Symbol thatAttrs = that.symbols()[0];
        final Symbol thatSrc = that.symbols()[1];
        assert thatAttrs != attrs;
        if (isEq(thatAttrs, attrs) && !checkSourceCompatible(source, thatSrc)) return false;
      }

      return true;
    }

    private boolean checkSourceCompatible(Symbol src0, Symbol src1) {
      if (src0 == null || src1 == null) return false;

      if (src1.kind() == src0.kind()) return isEq(src0, src1);
      if (src0.kind() == ATTRS) return checkSourceCompatible(directSourceOf(src0), src1);
      if (src1.kind() == ATTRS) return checkSourceCompatible(src0, directSourceOf(src1));

      return assertFalse();
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
      if (source != directSourceOf(attr)) return false;
      final Constraint me = constraints.get(index);
      final int begin = constraints.beginIndexOf(me.kind());

      boolean canImplyMe = false;
      for (int i = begin; i < index; ++i) if (canImply(constraints.get(i), me)) canImplyMe = true;

      if (!canImplyMe) return true;

      for (int i = begin; i < index; ++i)
        if (enabled[i] && canImply(constraints.get(i), me)) return true;

      return false;
    }

    @Override
    protected boolean canImply(Constraint pre, Constraint post) {
      final Symbol[] syms0 = pre.symbols();
      final Symbol[] syms1 = post.symbols();
      return isEq(syms0[1], syms1[1])
          && isEq(syms0[0], syms1[0])
          && directSourceOf(syms1[1]) == syms1[0];
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
      if (!(source0 == directSourceOf(attr0) && source1 == directSourceOf(attr1))) return false;

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
          && directSourceOf(attrs10) == src10
          && directSourceOf(attrs11) == src11;
    }
  }

  private class InstantiateEnumerator extends Enumerator {
    private final Symbol targetSym;
    private final List<Symbol> mappingSyms;

    protected InstantiateEnumerator(Symbol symbol, List<Symbol> symbols) {
      this.targetSym = symbol;
      this.mappingSyms = symbols;
    }

    @Override
    int enumerate() {
      byte[][] partitions = currPartitions.get(targetSym.kind());
      if (partitions == null) return CONFLICT;

      int result, finalResult = CONFLICT;
      //      final TIntList buffer = new TIntArrayList(mappingSyms.size());
      for (byte[] partition : partitions) {
        for (int i = 0, bound = partition.length; i < bound; ++i) {
          final int index = constraints.indexOfEq(mappingSyms.get(partition[i]), targetSym);
          enabled[index] = true;
          //          buffer.add(index);

          if (!(next instanceof InstantiateEnumerator)) partitionCount++;

          result = next.enumerate();
          if (result == TIMEOUT) return TIMEOUT;
          if (result == INCOMPLETE || result == EQ) finalResult = finalResult == EQ ? EQ : result;

          enabled[index] = false;
        }
        //        for (int i = 0, bound = buffer.size(); i < bound; ++i) enabled[buffer.get(i)] =
        // false;
        //        buffer.clear();
      }
      return finalResult;
    }
  }

  private class InstanceSourceChecker extends Enumerator {
    private final Symbols allSymbols;

    private InstanceSourceChecker(Symbols attrSyms) {
      allSymbols = attrSyms;
    }

    @Override
    int enumerate() {
      // Check symbol `indirect` sources, like SourceCheck
      for (Symbol symbol : allSymbols.symbolsOf(ATTRS))
        if (indirectSourceOf(symbol) == null) return CONFLICT;

      // Check whether exclusive instantiation (for TABLE and ATTR symbols)
      // TABLE: no 2 symbols in f1 are instantiated to the same symbol in f0
      List<Constraint> tableInstantiations = getInstantiations(TableEq);
      for (int i = 0, bound = tableInstantiations.size(); i < bound; i++)
        for (int j = i + 1; j < bound; j++)
          if (tableInstantiations.get(i).symbols()[0] == tableInstantiations.get(j).symbols()[0]) {
            return CONFLICT;
          }

      // ATTRS: a symbol in f1 cannot be instantiated to multiple symbols with diff sources (no need
      // to check now)

      return next.enumerate();
    }

    private List<Constraint> getInstantiations(Constraint.Kind kind) {
      assert kind.isEq();

      List<Constraint> instantiations = new ArrayList<>();
      for (int i = 0, bound = constraints.size(); i < bound; i++) {
        Constraint constraint = constraints.get(i);
        if (!enabled[i] || constraint.kind() != kind) continue;
        if (allSymbols.contains(constraint.symbols()[1])) instantiations.add(constraint);
      }
      return instantiations;
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
    private final int i = 0;

    @Override
    int enumerate() {
      final int answer = next.enumerate();

      if (answer == EQ) {
        final boolean[] result = Arrays.copyOf(enabled, enabled.length);
        for (boolean[] existRes : results) if (isWeakerThan(existRes, result)) return answer;

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
      try {
        return prove0(enabled);
      } catch (LeveledException ex) {
        if (ex.ignorable()) return CONFLICT;
        else throw ex;
      } finally {
        ++proverInvokeTimes;
      }
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
