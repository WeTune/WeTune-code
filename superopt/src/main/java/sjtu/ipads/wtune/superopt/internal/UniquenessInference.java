package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static com.google.common.collect.Lists.cartesianProduct;

public class UniquenessInference {
  public static boolean inferUniqueness(PlanNode node) {
    final List<List<AttributeDef>> cores = inferUniqueCores(node);
    return !cores.isEmpty();
  }

  private static List<List<AttributeDef>> inferUniqueCores(PlanNode node) {
    if (node instanceof InputNode) return inferUniqueCores((InputNode) node);
    else if (node instanceof JoinNode) return inferUniqueCores((JoinNode) node);
    else if (node instanceof PlainFilterNode) return inferUniqueCores(((PlainFilterNode) node));
    else if (node instanceof ProjNode) return inferUniqueCores((ProjNode) node);
    else if (node instanceof SubqueryFilterNode) return inferUniqueCores(node.predecessors()[0]);
    else {
      assert node.type().numPredecessors() == 1;
      return inferUniqueCores(node.predecessors()[0]);
    }
  }

  private static List<List<AttributeDef>> inferUniqueCores(InputNode input) {
    // Input: { unique_key }
    final Table table = input.table();
    final List<AttributeDef> attrs = input.definedAttributes();
    final List<List<AttributeDef>> cores = new LinkedList<>();

    for (Constraint constraint : table.constraints()) {
      if (constraint.type() != ConstraintType.UNIQUE && constraint.type() != ConstraintType.PRIMARY)
        continue;

      final List<AttributeDef> core = new LinkedList<>();
      cores.add(core);

      for (Column column : constraint.columns()) {
        final AttributeDef found = FuncUtils.find(it -> it.referredColumn() == column, attrs);
        assert found != null;
        core.add(found);
      }
    }

    return cores;
  }

  private static List<List<AttributeDef>> inferUniqueCores(JoinNode join) {
    // Join: { concat(l,r) | (l,r) \in left.cores x right.cores }
    final List<List<AttributeDef>> leftCores = inferUniqueCores(join.predecessors()[0]);
    final List<List<AttributeDef>> rightCores = inferUniqueCores(join.predecessors()[1]);

    final List<List<AttributeDef>> cores = new LinkedList<>();
    for (List<List<AttributeDef>> pair : cartesianProduct(leftCores, rightCores)) {
      final List<AttributeDef> leftCore = pair.get(0), rightCore = pair.get(1);

      if (join.isNormalForm()) {
        final List<AttributeDef> left = join.leftAttributes();
        final List<AttributeDef> right = join.rightAttributes();
        final List<AttributeDef> core0 = new LinkedList<>(rightCore);
        final List<AttributeDef> core1 = new LinkedList<>(leftCore);
        for (AttributeDef c : leftCore) if (!left.contains(c)) core0.add(c);
        for (AttributeDef c : rightCore) if (!right.contains(c)) core1.add(c);
        cores.add(core0);
        cores.add(core1);

      } else {
        leftCore.addAll(rightCore);
        cores.add(leftCore);
      }
    }

    return cores;
  }

  private static List<List<AttributeDef>> inferUniqueCores(PlainFilterNode node) {
    // PlainFilter: { { attr | attr \in inputCore /\ \exists `attr` = ? in node.expr }
    //                | inputCore \in inputCores }
    final List<List<AttributeDef>> inputCores = inferUniqueCores(node.predecessors()[0]);
    final Set<AttributeDef> toRemove = node.fixedValueAttributes();
    for (List<AttributeDef> inputCore : inputCores) inputCore.removeIf(toRemove::contains);
    return inputCores;
  }

  private static List<List<AttributeDef>> inferUniqueCores(ProjNode node) {
    final List<List<AttributeDef>> cores = inferUniqueCores(node.predecessors()[0]);
    final ListIterator<List<AttributeDef>> outerIter = cores.listIterator();
    final List<AttributeDef> projections = node.definedAttributes();

    while (outerIter.hasNext()) {
      final List<AttributeDef> core = outerIter.next();
      final ListIterator<AttributeDef> innerIter = core.listIterator();
      while (innerIter.hasNext()) {
        final AttributeDef attr = innerIter.next();
        final AttributeDef projection = FuncUtils.find(attr::equals, projections);
        if (projection != null) innerIter.set(attr);
        else {
          outerIter.remove();
          break;
        }
      }
    }

    if (node.isForcedUnique()) cores.add(projections);

    return cores;
  }
}
