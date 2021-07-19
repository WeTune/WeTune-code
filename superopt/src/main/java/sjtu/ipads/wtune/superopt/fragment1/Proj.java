package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan1.ValueBag;

public interface Proj extends Op {
  Symbol attrs();

  @Override
  default OperatorType type() {
    return OperatorType.PROJ;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.type() != type()) return false;
    return m.assign(attrs(), node.values());
  }

  @Override
  default PlanNode instantiate(PlanContext ctx, Model m) {
    final PlanNode predecessor = predecessors()[0].instantiate(ctx, m);

    final ProjNode proj = ProjNode.mk(ValueBag.mk(m.interpretAttrs(attrs())));

    proj.setPredecessor(0, predecessor);

    ctx.registerValues(proj, proj.values());
    ctx.registerRefs(proj, proj.refs());

    return Op.super.instantiate(ctx, m);
  }
}
