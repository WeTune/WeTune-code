package sjtu.ipads.wtune.superopt.optimizer.support;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.common.utils.EquivalentClasses;
import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.common.utils.LazySequence;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan.SortNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

public class UniquenessInference extends TypeBasedAlgorithm<InferenceStage> {
  private final EquivalentClasses<AttributeDef> equiKeys = new EquivalentClasses<>();
  private final InferenceStage[] EMPTY_PREV = new InferenceStage[0];

  public static boolean inferUniqueness(PlanNode node) {
    final InferenceStage stage = new UniquenessInference().dispatch(node);
    final Set<AttributeDef> next = stage.next();
    return next != null && !stage.isDrained();
  }

  @Override
  protected InferenceStage onInput(InputNode input) {
    return new InputStage(input, EMPTY_PREV);
  }

  @Override
  protected InferenceStage onInnerJoin(JoinNode innerJoin) {
    return new JoinStage(
        innerJoin,
        new InferenceStage[] {
          dispatch(innerJoin.predecessors()[0]), dispatch(innerJoin.predecessors()[1])
        },
        equiKeys);
  }

  @Override
  protected InferenceStage onLeftJoin(JoinNode leftJoin) {
    return new JoinStage(
        leftJoin,
        new InferenceStage[] {
          dispatch(leftJoin.predecessors()[0]), dispatch(leftJoin.predecessors()[1])
        },
        equiKeys);
  }

  @Override
  protected InferenceStage onPlainFilter(FilterNode filter) {
    return new PlainFilterStage(filter, new InferenceStage[] {dispatch(filter.predecessors()[0])});
  }

  @Override
  protected InferenceStage onSubqueryFilter(FilterNode filter) {
    return new SubqueryFilterStage(
        filter,
        new InferenceStage[] {
          dispatch(filter.predecessors()[0]), dispatch(filter.predecessors()[1])
        });
  }

  @Override
  protected InferenceStage onProj(ProjNode proj) {
    return new ProjStage(proj, new InferenceStage[] {dispatch(proj.predecessors()[0])});
  }

  @Override
  protected InferenceStage onAgg(AggNode agg) {
    return new AggStage(agg, new InferenceStage[] {dispatch(agg.predecessors()[0])});
  }

  @Override
  protected InferenceStage onLimit(LimitNode limit) {
    return new LimitStage(limit, new InferenceStage[] {dispatch(limit.predecessors()[0])});
  }

  @Override
  protected InferenceStage onSort(SortNode sort) {
    return new SortStage(sort, new InferenceStage[] {dispatch(sort.predecessors()[0])});
  }
}

abstract class InferenceStage implements LazySequence<Set<AttributeDef>> {
  protected boolean isDrained = false;
  protected Set<AttributeDef> current;

  protected final PlanNode node;
  protected final InferenceStage[] prev;

  private final List<Set<AttributeDef>> producedCores = new ArrayList<>();

  protected static final Set<AttributeDef> SINGLETON_RESULT_SET = new HashSet<>(0);
  protected static final Set<AttributeDef> RETRY = new HashSet<>(0);

  protected InferenceStage(PlanNode node, InferenceStage[] prev) {
    this.node = node;
    this.prev = prev;
  }

  protected boolean checkFreshness(Set<AttributeDef> newCore) {
    for (Set<AttributeDef> producedCore : producedCores)
      if (newCore.containsAll(producedCore)) return false;
    producedCores.add(newCore);
    return true;
  }

  @Override
  public Set<AttributeDef> next() {
    while (!isDrained) {
      final Set<AttributeDef> core = next0();
      if (core == null) {
        isDrained = true;
        break;
      }
      if (core == RETRY) continue;
      if (checkFreshness(core)) return current = core;
    }

    return null;
  }

  protected abstract Set<AttributeDef> next0();

  @Override
  public boolean isDrained() {
    return isDrained;
  }
}

class InputStage extends InferenceStage {
  private Iterator<Constraint> iter;

  protected InputStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
    assert node instanceof InputNode;
  }

  private Iterator<Constraint> iter() {
    if (iter != null) return iter;

    final List<Constraint> fks =
        listFilter(
            it -> ConstraintType.isUnique(it.type()),
            ((InputNode) this.node).table().constraints());

    fks.sort(Comparator.comparingInt(it -> it.columns().size()));

    return iter = fks.iterator();
  }

  @Override
  protected Set<AttributeDef> next0() {
    final Iterator<Constraint> iter = iter();
    if (!iter.hasNext()) return null;

    final Constraint fk = iter.next();
    final InputNode input = (InputNode) this.node;
    final List<AttributeDef> attrs = input.definedAttributes();

    final Set<AttributeDef> core = newIdentitySet(fk.columns().size());
    for (Column column : fk.columns()) {
      final AttributeDef found = FuncUtils.find(it -> it.referredColumn() == column, attrs);
      assert found != null;
      core.add(found);
    }

    return core;
  }
}

class JoinStage extends InferenceStage {
  private final EquivalentClasses<AttributeDef> equiClosures;

  private Set<AttributeDef> currentLeft;
  private Set<AttributeDef> currentRight;
  private Set<AttributeDef> currentSplitPivots;

  private LazySequence<Set<AttributeDef>> leftSeq;
  private LazySequence<Set<AttributeDef>> rightSeq;

  private List<Set<AttributeDef>> cachedRight;

  private Iterator<Set<AttributeDef>> splitPivotIter;

  private boolean firstForward = true;

  protected JoinStage(
      PlanNode node, InferenceStage[] prev, EquivalentClasses<AttributeDef> eqAttrs) {
    super(node, prev);
    this.equiClosures = eqAttrs;
  }

  private boolean forwardCore() {
    if (currentLeft == null) {
      // special case for the initial forward
      currentLeft = leftSeq.next();
      if (currentLeft == null || leftSeq.isDrained()) return false;
    }

    currentRight = rightSeq.next();

    if (rightSeq.isDrained()) {
      rightSeq = LazySequence.fromIterator(cachedRight.iterator()); // rewind the right
      currentRight = rightSeq.next(); // forward the right side
      currentLeft = leftSeq.next(); // forward the left side

      if (rightSeq.isDrained() || leftSeq.isDrained()) { // totally drained
        currentLeft = currentRight = null;
        return false;
      }
    }

    if (rightSeq == prev[1]) cachedRight.add(currentRight);

    // recalculate split pivots
    final AttributeDefBag leftAttrs = node.predecessors()[0].definedAttributes();
    final AttributeDefBag rightAttrs = node.predecessors()[1].definedAttributes();
    final Set<AttributeDef> leftPivot = calcSplitPivot(currentLeft, rightAttrs);
    final Set<AttributeDef> rightPivot = calcSplitPivot(currentRight, leftAttrs);
    if (leftPivot.isEmpty() && rightPivot.isEmpty()) splitPivotIter = null;
    else if (leftPivot.isEmpty()) splitPivotIter = singletonList(rightPivot).iterator();
    else if (rightAttrs.isEmpty()) splitPivotIter = singletonList(leftPivot).iterator();
    else splitPivotIter = List.of(leftPivot, rightPivot).iterator();

    return true;
  }

  private boolean forwardSplitPivots() {
    if (splitPivotIter != null && splitPivotIter.hasNext()) {
      currentSplitPivots = splitPivotIter.next();
      return true;
    }
    return false;
  }

  private boolean forward() {
    if (firstForward) {
      firstForward = false;
      leftSeq = prev[0];
      rightSeq = prev[1];
      cachedRight = new ArrayList<>(4);

      final JoinNode join = (JoinNode) node;
      if (join.isNormalForm())
        zipForEach(this::addEqualPair, join.leftAttributes(), join.rightAttributes());
    }

    if (!forwardSplitPivots()) {
      // the first invocation of `forward` must reach here
      // and trigger the initial `forwardCore`
      if (!forwardCore()) return false;
      // the iter of split pivot is rewound, forward it
      forwardSplitPivots();
      return true;
    }

    return true;
  }

  @Override
  protected Set<AttributeDef> next0() {
    if (!forward()) return null;

    final Set<AttributeDef> leftCore = currentLeft;
    final Set<AttributeDef> rightCore = currentRight;
    final Set<AttributeDef> splitPivot = currentSplitPivots;

    assert currentLeft != null && currentRight != null;
    if (leftCore == SINGLETON_RESULT_SET) return rightCore;
    if (rightCore == SINGLETON_RESULT_SET) return leftCore;

    final Set<AttributeDef> newCore = newIdentitySet(leftCore.size() + rightCore.size());
    newCore.addAll(leftCore);
    newCore.addAll(rightCore);

    if (splitPivot != null && ((JoinNode) node).isNormalForm()) newCore.removeAll(splitPivot);

    return newCore;
  }

  private void addEqualPair(AttributeDef x, AttributeDef y) {
    equiClosures.makeClass(x).add(y);
  }

  private Set<AttributeDef> calcSplitPivot(Set<AttributeDef> attrs, AttributeDefBag counterpart) {
    final Set<AttributeDef> splitPivot = newIdentitySet();

    for (AttributeDef attr : attrs) {
      final Set<AttributeDef> equiClosure = equiClosures.get(attr);
      if (equiClosure == null) continue;
      for (AttributeDef counterpartAttr : counterpart)
        if (equiClosure.contains(counterpartAttr)) {
          splitPivot.add(attr);
          break;
        }
    }

    return splitPivot;
  }

  private Collection<Set<AttributeDef>> calcSplitPivots() {
    final JoinNode join = (JoinNode) this.node;
    if (!join.isNormalForm()) return emptyList();

    final List<AttributeDef> left = join.leftAttributes();
    final List<AttributeDef> right = join.rightAttributes();

    zipForEach(this::addEqualPair, left, right);

    /*
     Assume: A JOIN B on a.x = b.y. Ua is A's unique core, Ub is B's unique core

     After cartesian product, the core is
         Ua \cup Ub.

     We can add a.x and b.y to the core:
         Ua \cup Ub \cup {a.x} \cup {b.y},
     this is still a unique (although not minimal).

     Since we know the value of a.x must equals to b.y in each tuple,
     it can be reduced to:
         (Ua \cup Ub \cup {a.x} \cup {b.y}) - {a.x}
       = (Ua - {a.x}) \cup Ub \cup {b.y}
     and symmetrically:
         Ua \cup (Ub - {b.y}) \cup {a.x}

     Moreover, since Ua/Ub is the original unique core, then the value of a.x/b.y
     can be uniquely determined by Ua in each tuple. Thus {a.x}/{b.y} can also be reduced.

     The final new core should be:
       (Ua \cup Ub) - {a.x}  or  (Ua \cup Ub) - {b.y}
    */
    /*
     Based on the induction above, we use `splitPivots` to calculate the new cores.

     We defines the attributes whose value is inferred equal to another attributes
     (according to equi-join key) as "equi-attributes".
     Each equi-attribute belongs to a equi-closure.

     The `splitPivots` is a set of `splitPivot`.
     Each `splitPivot` is a set of attributes, containing exact one attribute of
     every equi-closure.

     Example-1: A JOIN B ON A.x = B.y, the splitPivots is { {A.x}, {B.y} }
     Example-2: A JOIN B ON A.x = B.y JOIN C ON B.y = C.z JOIN D ON C.u = D.w
     the splitPivots is { {A.x,C.u}, {B.y,C.u}, {C.z,C.u}, {A.x,D.w}, {B.y,D.w}, {C.u,D.w}}

     Afterwards, each splitPivot are removed from $leftCores \cup rightCores$, the resultant
     sets are new cores.
    */

    final Collection<Set<AttributeDef>> splitPivots = calcSplitPivots0(left);
    assert splitPivots.size() > 1;

    return splitPivots;
  }

  private Collection<Set<AttributeDef>> calcSplitPivots0(List<AttributeDef> oneSide) {
    final List<Set<AttributeDef>> pivots = new ArrayList<>(1 << oneSide.size());
    return calcSplitPivots0(oneSide, 0, newIdentitySet(), pivots);
  }

  private Collection<Set<AttributeDef>> calcSplitPivots0(
      List<AttributeDef> oneSide,
      int i,
      Set<AttributeDef> diff,
      Collection<Set<AttributeDef>> diffs) {
    if (i >= oneSide.size()) {
      diffs.add(diff);
      return diffs;
    }

    final AttributeDef def = oneSide.get(i);
    final Set<AttributeDef> eqDefs = equiClosures.get(def);

    for (AttributeDef eqDef : eqDefs) {
      final Set<AttributeDef> newDiff = newIdentitySet(diff);
      newDiff.add(eqDef);
      calcSplitPivots0(oneSide, i + 1, newDiff, diffs);
    }

    return diffs;
  }
}

class PlainFilterStage extends InferenceStage {
  protected PlainFilterStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
  }

  @Override
  protected Set<AttributeDef> next0() {
    Set<AttributeDef> core = prev[0].next();
    if (core == null || prev[0].isDrained()) return null;

    core = newIdentitySet(core);
    if (core != SINGLETON_RESULT_SET) {
      core.removeAll(((FilterNode) node).fixedValueAttributes());
      if (core.isEmpty()) return SINGLETON_RESULT_SET;
    }

    return core;
  }
}

class SubqueryFilterStage extends InferenceStage {
  protected SubqueryFilterStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
  }

  @Override
  protected Set<AttributeDef> next0() {
    final Set<AttributeDef> core = prev[0].next();
    if (core == null || prev[0].isDrained()) return null;
    return core;
  }
}

class ProjStage extends InferenceStage {
  private TIntList retained;
  private boolean firstTime = true;

  protected ProjStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
  }

  private TIntList retained() {
    if (retained != null) return retained;

    final List<AttributeDef> projections = node.definedAttributes();
    final TIntList retained = new TIntArrayList(projections.size());
    for (AttributeDef projection : projections) {
      final AttributeDef upstream = projection.upstream();
      retained.add(upstream == null ? -1 : upstream.id());
    }
    return this.retained = retained;
  }

  @Override
  protected Set<AttributeDef> next0() {
    final ProjNode proj = (ProjNode) this.node;
    if (firstTime) {
      firstTime = false;
      if (proj.isForcedUnique()) return newIdentitySet(proj.usedAttributes());
    }

    final Set<AttributeDef> core = prev[0].next();

    if (core == null || prev[0].isDrained()) return null;
    if (core == SINGLETON_RESULT_SET) return core;
    if (proj.usedAttributes().isEmpty()) return SINGLETON_RESULT_SET; // heuristic for COUNT(1)

    final TIntList retained = retained();
    for (AttributeDef coreAttr : core)
      if (!retained.contains(coreAttr.id()))
        return RETRY; // the outer loop (in InferenceStage::next) will retry

    return core;
  }
}

class AggStage extends InferenceStage {

  protected AggStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
  }

  @Override
  protected Set<AttributeDef> next0() {
    if (((AggNode) node).groups().isEmpty()) {
      isDrained = true;
      return SINGLETON_RESULT_SET;
    }
    return null;
  }
}

class LimitStage extends InferenceStage {
  private final boolean isSingleton;

  protected LimitStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
    isSingleton = Integer.valueOf(1).equals(((LimitNode) node).limit().get(LITERAL_VALUE));
  }

  @Override
  protected Set<AttributeDef> next0() {
    if (isSingleton) {
      isDrained = true;
      return SINGLETON_RESULT_SET;
    }
    return prev[0].next();
  }
}

class SortStage extends InferenceStage {

  protected SortStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
  }

  @Override
  protected Set<AttributeDef> next0() {
    return prev[0].next();
  }
}
