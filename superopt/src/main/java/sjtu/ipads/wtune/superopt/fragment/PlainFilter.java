package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.internal.PlainFilterImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public interface PlainFilter extends Operator {
  Placeholder fields();

  Placeholder predicate();

  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static PlainFilter create() {
    return PlainFilterImpl.create();
  }

  @Override
  default PlanNode instantiate(Interpretations interpretations) {
    final PlanNode predecessor = predecessors()[0].instantiate(interpretations);
    final PlanNode node =
        PlainFilterNode.make(
            interpretations.getPredicate(predicate()).object(),
            interpretations.getAttributes(fields()).object());
    node.setPredecessor(0, predecessor);
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (!node.type().isFilter()) return false;
    final FilterNode filter = (FilterNode) node;
    return inter.assignAttributes(fields(), filter.usedAttributes())
        && inter.assignPredicate(predicate(), filter.expr());
  }
}
