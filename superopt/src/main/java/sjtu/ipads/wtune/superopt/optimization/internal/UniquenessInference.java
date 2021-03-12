package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.common.utils.EquivalentClasses;
import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.*;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.emptySet;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;

public class UniquenessInference extends TypeBasedAlgorithm<UniquenessCore> {
  private final EquivalentClasses<AttributeDef> eqAttrs = new EquivalentClasses<>();

  public static boolean inferUniqueness(PlanNode node) {
    final UniquenessCore core = new UniquenessInference().dispatch(node);
    return core.isSingleton() || !core.attrs().isEmpty();
  }

  @Override
  protected UniquenessCore onInput(InputNode input) {
    // Input: { unique_key }
    final Table table = input.table();
    final List<AttributeDef> attrs = input.definedAttributes();
    final Set<Set<AttributeDef>> cores = new HashSet<>();

    for (Constraint constraint : table.constraints()) {
      if (constraint.type() != ConstraintType.UNIQUE && constraint.type() != ConstraintType.PRIMARY)
        continue;

      final Set<AttributeDef> core = new HashSet<>();

      for (Column column : constraint.columns()) {
        final AttributeDef found = FuncUtils.find(it -> it.referredColumn() == column, attrs);
        assert found != null;
        core.add(found);
      }

      cores.add(core);
    }

    return new UniquenessCore(cores, false);
  }

  @Override
  protected UniquenessCore onLeftJoin(LeftJoinNode join) {
    return handleJoin(join);
  }

  @Override
  protected UniquenessCore onInnerJoin(InnerJoinNode join) {
    return handleJoin(join);
  }

  @Override
  protected UniquenessCore onPlainFilter(PlainFilterNode node) {
    // PlainFilter: { { attr | attr \in inputCore /\ \exists `attr` = ? in node.expr }
    //                | inputCore \in inputCores }
    final UniquenessCore cores = dispatch(node.predecessors()[0]);
    if (cores.isSingleton()) return cores;

    return removeAttributesFromCores(cores, node.fixedValueAttributes());
  }

  @Override
  protected UniquenessCore onSubqueryFilter(SubqueryFilterNode filter) {
    final UniquenessCore cores = dispatch(filter.predecessors()[0]);
    if (cores.isSingleton()) return cores;

    final UniquenessCore subQueryCore = dispatch(filter.predecessors()[1]);
    if (subQueryCore.isSingleton())
      return removeAttributesFromCores(cores, filter.usedAttributes());

    return cores;
  }

  private UniquenessCore removeAttributesFromCores(
      UniquenessCore cores, Collection<AttributeDef> toRemove) {
    final Set<Set<AttributeDef>> newCores = new HashSet<>(cores.attrs().size());
    for (Set<AttributeDef> core : cores.attrs()) {
      final HashSet<AttributeDef> newCore = new HashSet<>(core);
      newCore.removeAll(toRemove);
      newCores.add(newCore);
    }

    return new UniquenessCore(newCores, false);
  }

  @Override
  protected UniquenessCore onProj(ProjNode node) {
    final UniquenessCore cores = dispatch(node.predecessors()[0]);
    if (cores.isSingleton()) return cores;
    if (node.usedAttributes().isEmpty()) return new UniquenessCore(emptySet(), true); // heuristic

    final List<AttributeDef> projections = node.definedAttributes();

    cores.attrs().removeIf(it -> !projections.containsAll(it));
    if (node.isForcedUnique()) cores.attrs().add(new HashSet<>(projections));

    return cores;
  }

  @Override
  protected UniquenessCore onAgg(AggNode agg) {
    return new UniquenessCore(emptySet(), agg.groupKeys().isEmpty());
  }

  @Override
  protected UniquenessCore onSort(SortNode sort) {
    return dispatch(sort.predecessors()[0]);
  }

  @Override
  protected UniquenessCore onLimit(LimitNode limit) {
    final ASTNode expr = limit.limit();
    if (expr.getOr(LITERAL_VALUE, null) == Integer.valueOf(1))
      return new UniquenessCore(emptySet(), true);
    else return dispatch(limit.predecessors()[0]);
  }

  private UniquenessCore handleJoin(JoinNode join) {
    // Join: { concat(l,r) | (l,r) \in left.cores x right.cores }
    final UniquenessCore leftCores = dispatch(join.predecessors()[0]);
    final UniquenessCore rightCores = dispatch(join.predecessors()[1]);

    if (leftCores.isSingleton()) return rightCores;
    if (rightCores.isSingleton()) return leftCores;

    final Collection<Set<AttributeDef>> splitPivots;
    if (join.isNormalForm()) {
      final List<AttributeDef> left = join.leftAttributes();
      final List<AttributeDef> right = join.rightAttributes();

      zipForEach(this::addEqAttr, left, right);
      splitPivots = calcSplitPivots(left);
    } else splitPivots = null;

    final Set<Set<AttributeDef>> cores = new HashSet<>();
    for (var pair : cartesianProduct(leftCores.attrs(), rightCores.attrs())) {
      final Set<AttributeDef> leftCore = pair.get(0), rightCore = pair.get(1);

      final Set<AttributeDef> newCore = new HashSet<>(leftCore.size() + rightCore.size());
      newCore.addAll(leftCore);
      newCore.addAll(rightCore);

      if (join.isNormalForm()) {
        // Assume: A JOIN B on a.x = b.y. Ua is A's unique core, Ub is B's unique core
        //
        // After cartesian product, the core is
        //     Ua \cup Ub.
        //
        // We can add a.x and b.y to the core:
        //     Ua \cup Ub \cup {a.x} \cup {b.y},
        // this is still a unique (although not minimal).
        //
        // Since we know the value of a.x must equals to b.y in each tuple,
        // it can be reduced to:
        //    (Ua \cup Ub \cup {a.x} \cup {b.y}) - {a.x}
        //  = (Ua - {a.x}) \cup Ub \cup {b.y}
        // and symmetrically:
        //    Ua \cup (Ub - {b.y}) \cup {a.x}
        //
        // Moreover, since Ua/Ub is the original unique core, then the value of a.x/b.y
        // can be uniquely determined by Ua in each tuple. Thus {a.x}/{b.y} can also be reduced.
        //
        // The final new core should be:
        //  (Ua \cup Ub) - {a.x}  or  (Ua \cup Ub) - {b.y}
        assert splitPivots != null;
        for (Set<AttributeDef> pivot : splitPivots) {
          final Set<AttributeDef> coreCopy = new HashSet<>(newCore);
          coreCopy.removeAll(pivot);
          cores.add(coreCopy);
        }

      } else cores.add(newCore);
    }

    return new UniquenessCore(cores, false);
  }

  private void addEqAttr(AttributeDef x, AttributeDef y) {
    eqAttrs.makeClass(x).add(y);
  }

  private boolean canSplit(Set<AttributeDef> core, Set<AttributeDef> pivots) {
    outer:
    for (AttributeDef pivot : pivots) {
      final Set<AttributeDef> eqDefs = eqAttrs.get(pivot);
      for (AttributeDef def : core)
        if (eqDefs.contains(def) && !pivots.contains(def)) continue outer;
      return false;
    }

    return true;
  }

  private Collection<Set<AttributeDef>> calcSplitPivots(List<AttributeDef> oneSide) {
    final List<Set<AttributeDef>> pivots = new ArrayList<>(1 << oneSide.size());
    calcSplitPivots(new HashSet<>(), oneSide, 0, pivots);
    return pivots;
  }

  private void calcSplitPivots(
      Set<AttributeDef> diff,
      List<AttributeDef> eqSide,
      int i,
      Collection<Set<AttributeDef>> dest) {
    if (i >= eqSide.size()) {
      dest.add(diff);
      return;
    }

    final AttributeDef def = eqSide.get(i);
    final Set<AttributeDef> eqDefs = eqAttrs.get(def);

    for (AttributeDef eqDef : eqDefs) {
      final Set<AttributeDef> newDiff = eqDef == def ? diff : new HashSet<>(diff);
      newDiff.add(eqDef);
      calcSplitPivots(newDiff, eqSide, i + 1, dest);
    }
  }
}
