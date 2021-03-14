package sjtu.ipads.wtune.superopt.optimization.internal;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.EquivalentClasses;
import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.cartesianProduct;
import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;

public class UniquenessInference extends TypeBasedAlgorithm<Uniqueness> {
  private final EquivalentClasses<AttributeDef> equalAttrs = new EquivalentClasses<>();

  public static boolean inferUniqueness(PlanNode node) {
    if (node.type() == Proj && ((ProjNode) node).isForcedUnique()) return true; // a short cut

    final Uniqueness core = new UniquenessInference().dispatch(node);
    return core.isSingleton() || !core.cores().isEmpty();
  }

  @Override
  protected Uniqueness onInput(InputNode input) {
    // Input: { unique_key }
    final Table table = input.table();
    final List<AttributeDef> attrs = input.definedAttributes();
    final List<Set<AttributeDef>> cores = new ArrayList<>(4);

    for (Constraint constraint : table.constraints()) {
      if (constraint.type() != ConstraintType.UNIQUE && constraint.type() != ConstraintType.PRIMARY)
        continue;

      final Set<AttributeDef> core = newIdentitySet();
      cores.add(core);

      for (Column column : constraint.columns()) {
        final AttributeDef found = FuncUtils.find(it -> it.referredColumn() == column, attrs);
        assert found != null;
        core.add(found);
      }
    }

    reduceCores(cores);
    return new Uniqueness(cores, false);
  }

  @Override
  protected Uniqueness onLeftJoin(LeftJoinNode join) {
    return calcUniqueCoreOfJoin(join);
  }

  @Override
  protected Uniqueness onInnerJoin(InnerJoinNode join) {
    return calcUniqueCoreOfJoin(join);
  }

  @Override
  protected Uniqueness onPlainFilter(PlainFilterNode node) {
    // PlainFilter: { { attr | attr \in inputCore /\ \exists `attr` = ? in node.expr }
    //                | inputCore \in inputCores }
    final Uniqueness uniqueness = dispatch(node.predecessors()[0]);
    if (!uniqueness.isSingleton())
      removeAttributesFromCores(uniqueness.cores(), node.fixedValueAttributes());
    return uniqueness;
  }

  @Override
  protected Uniqueness onSubqueryFilter(SubqueryFilterNode filter) {
    final Uniqueness uniqueness = dispatch(filter.predecessors()[0]);
    if (!uniqueness.isSingleton() && dispatch(filter.predecessors()[1]).isSingleton())
      removeAttributesFromCores(uniqueness.cores(), filter.usedAttributes());
    return uniqueness;
  }

  @Override
  protected Uniqueness onProj(ProjNode node) {
    final Uniqueness uniqueness = dispatch(node.predecessors()[0]);
    if (uniqueness.isSingleton()) return uniqueness;
    if (node.usedAttributes().isEmpty()) return new Uniqueness(emptyList(), true); // heuristic

    final List<AttributeDef> projections = node.definedAttributes();

    final TIntList retained = new TIntArrayList(projections.size());
    for (AttributeDef projection : projections) {
      final AttributeDef upstream = projection.upstream();
      retained.add(upstream == null ? -1 : upstream.id());
    }

    final List<Set<AttributeDef>> newCores = new ArrayList<>(uniqueness.cores().size());

    outer:
    for (Set<AttributeDef> core : uniqueness.cores()) {
      final Set<AttributeDef> newCore = newIdentitySet(core.size());
      for (AttributeDef coreAttr : core) {
        final int idx = retained.indexOf(coreAttr.id());
        if (idx == -1) continue outer;
        newCore.add(projections.get(idx));
      }
      newCores.add(newCore);
    }

    if (node.isForcedUnique()) newCores.add(newIdentitySet(projections));
    // no need to reduce core
    return new Uniqueness(newCores, false);
  }

  @Override
  protected Uniqueness onAgg(AggNode agg) {
    return new Uniqueness(emptyList(), agg.groupKeys().isEmpty());
  }

  @Override
  protected Uniqueness onSort(SortNode sort) {
    return dispatch(sort.predecessors()[0]);
  }

  @Override
  protected Uniqueness onLimit(LimitNode limit) {
    final ASTNode expr = limit.limit();
    if (Integer.valueOf(1).equals(expr.getOr(LITERAL_VALUE, null)))
      return new Uniqueness(emptyList(), true);
    else return dispatch(limit.predecessors()[0]);
  }

  private void addEqualPair(AttributeDef x, AttributeDef y) {
    equalAttrs.makeClass(x).add(y);
  }

  private Uniqueness calcUniqueCoreOfJoin(JoinNode join) {
    // Join: { concat(l,r) | (l,r) \in left.cores x right.cores }
    final Uniqueness leftCores = dispatch(join.predecessors()[0]);
    final Uniqueness rightCores = dispatch(join.predecessors()[1]);

    if (leftCores.isSingleton()) return rightCores;
    if (rightCores.isSingleton()) return leftCores;

    final Collection<Set<AttributeDef>> splitPivots;
    if (join.isNormalForm()) {
      final List<AttributeDef> left = join.leftAttributes();
      final List<AttributeDef> right = join.rightAttributes();

      zipForEach(this::addEqualPair, left, right);
      splitPivots = calcSplitPivots(left);
      assert splitPivots.size() > 1;
    } else splitPivots = null;

    final List<Set<AttributeDef>> leftAttrs = leftCores.cores();
    final List<Set<AttributeDef>> rightAttrs = rightCores.cores();
    final List<Set<AttributeDef>> cores = new ArrayList<>(leftAttrs.size() * rightAttrs.size());

    for (var pair : cartesianProduct(leftAttrs, rightAttrs)) {
      final Set<AttributeDef> leftCore = pair.get(0), rightCore = pair.get(1);

      final Set<AttributeDef> newCore = newIdentitySet(leftCore.size() + rightCore.size());
      newCore.addAll(leftCore);
      newCore.addAll(rightCore);

      if (join.isNormalForm()) {
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

        assert splitPivots != null;
        for (Set<AttributeDef> pivot : splitPivots) {
          final Set<AttributeDef> coreCopy = newIdentitySet(newCore);
          coreCopy.removeAll(pivot);
          cores.add(coreCopy);
        }

      } else cores.add(newCore);
    }

    reduceCores(cores);
    return new Uniqueness(cores, false);
  }

  private Collection<Set<AttributeDef>> calcSplitPivots(List<AttributeDef> oneSide) {
    final List<Set<AttributeDef>> pivots = new ArrayList<>(1 << oneSide.size());
    calcSplitPivots(newIdentitySet(), oneSide, 0, pivots);
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
    final Set<AttributeDef> eqDefs = equalAttrs.get(def);

    for (AttributeDef eqDef : eqDefs) {
      final Set<AttributeDef> newDiff = eqDef == def ? diff : newIdentitySet(diff);
      newDiff.add(eqDef);
      calcSplitPivots(newDiff, eqSide, i + 1, dest);
    }
  }

  private static void removeAttributesFromCores(
      List<Set<AttributeDef>> cores, Collection<AttributeDef> toRemove) {
    cores.forEach(it -> it.removeAll(toRemove));
    reduceCores(cores);
  }

  private static void reduceCores(List<Set<AttributeDef>> diff) {
    final int totalSize = diff.size();
    int bound = totalSize;
    for (int i = 0; i < bound; i++) {
      final Set<AttributeDef> x = diff.get(i);
      for (int j = i + 1; j < bound; ) {
        final Set<AttributeDef> y = diff.get(j);
        if (y.containsAll(x)) {
          diff.set(j, diff.get(bound - 1));
          --bound;
          assert bound >= j;
        } else ++j;
      }
    }

    if (bound < totalSize) diff.subList(bound, totalSize).clear();
  }
}
