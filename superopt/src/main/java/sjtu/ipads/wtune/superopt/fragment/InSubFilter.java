package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.List;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.IterableSupport.zip;
import static sjtu.ipads.wtune.common.utils.LeveledException.ignorableEx;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.bindValuesRelaxed;

public interface InSubFilter extends AttrsFilter {
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
    final List<Value> values =
        bindValuesRelaxed(m.interpretInAttrs(attrs()), m.planContext(), predecessor0);

    if (values.size() != predecessor1.values().size()) {
      throw ignorableEx("Ill-formed subquery");
    }

    final List<Ref> refs = ListSupport.map((Iterable<Value>) values, (Function<? super Value, ? extends Ref>) Value::selfish);
    final InSubFilterNode f = InSubFilterNode.mk(RefBag.mk(refs));

    f.setContext(ctx);
    f.setPredecessor(0, predecessor0);
    f.setPredecessor(1, predecessor1);

    ctx.registerRefs(f, f.refs());
    zip(refs, values, ctx::setRef);

    return f;
  }
}
