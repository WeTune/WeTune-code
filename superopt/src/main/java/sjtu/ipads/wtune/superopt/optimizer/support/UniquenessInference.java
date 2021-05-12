package sjtu.ipads.wtune.superopt.optimizer.support;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
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
import sjtu.ipads.wtune.sqlparser.plan.TypeBasedAlgorithm;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

public class UniquenessInference extends TypeBasedAlgorithm<InferenceStage> {
  private final EquivalentClasses<AttributeDef> equiKeys = new EquivalentClasses<>();
  private final InferenceStage[] EMPTY_PREV = new InferenceStage[0];

  public static boolean inferUniqueness(PlanNode node) {
    final InferenceStage stage = new UniquenessInference().dispatch(node);
    final Set<AttributeDef> next = stage.next();
    //    return next != null && !stage.isDrained();
    return next != null;
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
  private TIntObjectMap<AttributeDef> retained;
  private boolean firstTime = true;

  protected ProjStage(PlanNode node, InferenceStage[] prev) {
    super(node, prev);
  }

  private TIntObjectMap<AttributeDef> retained() {
    if (retained != null) return retained;

    final List<AttributeDef> projections = node.definedAttributes();
    final TIntObjectMap<AttributeDef> retained = new TIntObjectHashMap<>(projections.size());

    for (final AttributeDef projection : projections) {
      final AttributeDef upstream = projection.upstream();
      if (upstream != null) retained.put(upstream.id(), projection);
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

    final TIntObjectMap<AttributeDef> retained = retained();
    final Set<AttributeDef> newCore = newIdentitySet(core.size());

    for (AttributeDef coreAttr : core) {
      final AttributeDef projected = retained.get(coreAttr.id());
      if (projected == null) return RETRY; // the outer loop (in InferenceStage::next) will retry
      newCore.add(projected);
    }

    return newCore;
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
    return newIdentitySet(node.definedAttributes());
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
