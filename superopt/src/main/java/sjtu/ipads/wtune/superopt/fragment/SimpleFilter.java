package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.bindValuesRelaxed;

public interface SimpleFilter extends Filter {
  Symbol predicate();

  @Override
  default OperatorType kind() {
    return OperatorType.SIMPLE_FILTER;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.kind() != kind()) return false;

    final SimpleFilterNode f = (SimpleFilterNode) node;
    return m.assign(predicate(), f.predicate()) && m.assign(attrs(), f.context().deRef(f.refs()));
  }

  @Override
  default PlanNode instantiate(Model m, PlanContext ctx) {
    final PlanNode predecessor = predecessors()[0].instantiate(m, ctx);

    final Expr predicate = m.interpretPred(predicate());
    final List<Value> values =
        bindValuesRelaxed(m.interpretInAttrs(attrs()), m.planContext(), predecessor);
    final List<Ref> refs = listMap(values, Value::selfish);
    final SimpleFilterNode f = SimpleFilterNode.mk(predicate, RefBag.mk(refs));

    f.setContext(ctx);
    f.setPredecessor(0, predecessor);

    ctx.registerRefs(f, f.refs());
    zipForEach(refs, values, ctx::setRef);

    return f;
  }
}
