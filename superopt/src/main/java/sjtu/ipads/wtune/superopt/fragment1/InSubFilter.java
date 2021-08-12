package sjtu.ipads.wtune.superopt.fragment1;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.bindValues;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.InSubFilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Ref;
import sjtu.ipads.wtune.sqlparser.plan1.RefBag;
import sjtu.ipads.wtune.sqlparser.plan1.Value;

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
    final PlanNode predecessor0 = predecessors()[0].instantiate(ctx, m);
    final PlanNode predecessor1 = predecessors()[1].instantiate(ctx, m);
    final List<Value> values = bindValues(m.interpretAttrs(attrs()), predecessor0);
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
