package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;

public interface InSubFilter extends Filter {
  @Override
  default OperatorType type() {
    return OperatorType.IN_SUB_FILTER;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.type() != type()) return false;

    final InSubFilterNode f = (InSubFilterNode) node;
    return m.assign(attrs(), f.context().deRef(f.lhsRefs()));
  }

  @Override
  default PlanNode instantiate(PlanContext ctx, Model m) {
    final PlanNode predecessor = predecessors()[0].instantiate(ctx, m);

    final List<Value> values = m.interpretAttrs(attrs());
    final List<Ref> refs = listMap(values, Value::selfish);
    final InSubFilterNode f = InSubFilterNode.mk(RefBag.mk(refs));

    f.setPredecessor(0, predecessor);

    ctx.registerRefs(f, f.refs());
    zipForEach(refs, values, ctx::setRef);

    return f;
  }
}
