package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.List;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.bindValuesRelaxed;

public interface Join extends Op {
  Symbol lhsAttrs();

  Symbol rhsAttrs();

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.kind() != this.kind()) return false;

    final JoinNode join = (JoinNode) node;
    if (!join.isEquiJoin()) return false;

    final PlanContext ctx = join.context();

    return m.assign(lhsAttrs(), ctx.deRef(join.lhsRefs()))
        && m.assign(rhsAttrs(), ctx.deRef(join.rhsRefs()));
  }

  @Override
  default PlanNode instantiate(Model m, PlanContext ctx) {
    final PlanNode predecessor0 = predecessors()[0].instantiate(m, ctx);
    final PlanNode predecessor1 = predecessors()[1].instantiate(m, ctx);

    final List<Value> lhsValues =
        bindValuesRelaxed(m.interpretInAttrs(lhsAttrs()), m.planContext(), predecessor0);
    final List<Value> rhsValues =
        bindValuesRelaxed(m.interpretInAttrs(rhsAttrs()), m.planContext(), predecessor1);
    final List<Ref> lhsRefs = ListSupport.map((Iterable<Value>) lhsValues, (Function<? super Value, ? extends Ref>) Value::selfish);
    final List<Ref> rhsRefs = ListSupport.map((Iterable<Value>) rhsValues, (Function<? super Value, ? extends Ref>) Value::selfish);
    final JoinNode join = JoinNode.mk(kind(), RefBag.mk(lhsRefs), RefBag.mk(rhsRefs));

    join.setContext(ctx);
    join.setPredecessor(0, predecessor0);
    join.setPredecessor(1, predecessor1);

    ctx.registerRefs(join, join.lhsRefs());
    ctx.registerRefs(join, join.rhsRefs());
    zipForEach(lhsRefs, lhsValues, ctx::setRef);
    zipForEach(rhsRefs, rhsValues, ctx::setRef);

    return join;
  }
}
