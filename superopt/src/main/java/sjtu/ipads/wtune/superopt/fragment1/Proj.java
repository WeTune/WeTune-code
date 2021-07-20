package sjtu.ipads.wtune.superopt.fragment1;

import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.bindValues;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;
import sjtu.ipads.wtune.sqlparser.plan1.ValueBag;

public interface Proj extends Op {
  Symbol inAttrs();

  Symbol outAttrs();

  @Override
  default OperatorType type() {
    return OperatorType.PROJ;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.type() != type()) return false;

    return m.assign(inAttrs(), node.context().deRef(node.refs()))
        && m.assign(outAttrs(), node.values());
  }

  @Override
  default PlanNode instantiate(PlanContext ctx, Model m) {
    final PlanNode predecessor = predecessors()[0].instantiate(ctx, m);
    final ProjNode proj = ProjNode.mk(ValueBag.mk(m.interpretAttrs(outAttrs())));

    proj.setContext(ctx);
    proj.setPredecessor(0, predecessor);

    ctx.registerValues(proj, proj.values());
    ctx.registerRefs(proj, proj.refs());

    final List<Value> usedValues = bindValues(m.interpretAttrs(inAttrs()), predecessor);
    zipForEach(proj.refs(), usedValues, ctx::setRef);

    return proj;
  }
}
