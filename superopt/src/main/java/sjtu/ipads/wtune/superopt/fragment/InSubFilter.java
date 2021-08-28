package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.superopt.fragment.FragmentUtils.bindValues;

public interface InSubFilter extends Filter {
  @Override
  default OperatorType kind() {
    return OperatorType.IN_SUB_FILTER;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.kind() != kind()) return false;

    final InSubFilterNode f = (InSubFilterNode) node;
    return m.assign(attrs(), f.context().deRef(f.lhsRefs()));
  }

  @Override
  default PlanNode instantiate(Model m, PlanContext ctx) {
    final PlanNode predecessor0 = predecessors()[0].instantiate(m, ctx);
    final PlanNode predecessor1 = predecessors()[1].instantiate(m, ctx);
    final List<Value> values = bindValues(m.interpretInAttrs(attrs()), predecessor0);
    final List<Ref> refs = listMap(values, Value::selfish);
    final InSubFilterNode f = InSubFilterNode.mk(RefBag.mk(refs));

    f.setContext(ctx);
    f.setPredecessor(0, predecessor0);
    f.setPredecessor(1, predecessor1);

    ctx.registerRefs(f, f.refs());
    zipForEach(refs, values, ctx::setRef);

    return f;
  }
}
