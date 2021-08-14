package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.bindValues;

public interface Proj extends Op {
  Symbol inAttrs();

  Symbol outAttrs();

  void setDeduplicated(boolean flag);

  boolean isDeduplicated();

  @Override
  default OperatorType kind() {
    return OperatorType.PROJ;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node.kind() != kind()) return false;
    if (isDeduplicated() != ((ProjNode) node).isDeduplicated()) return false;

    return m.assign(inAttrs(), node.context().deRef(node.refs()))
        && m.assign(outAttrs(), node.values());
  }

  @Override
  default PlanNode instantiate(Model m, PlanContext ctx) {
    final PlanNode predecessor = predecessors()[0].instantiate(m, ctx);
    final ProjNode proj = ProjNode.mk(ValueBag.mk(m.interpretAttrs(outAttrs())));
    proj.setDeduplicated(isDeduplicated());

    proj.setContext(ctx);
    proj.setPredecessor(0, predecessor);

    ctx.registerValues(proj, proj.values());
    ctx.registerRefs(proj, proj.refs());

    final List<Value> usedValues = bindValues(m.interpretAttrs(inAttrs()), predecessor);
    zipForEach(proj.refs(), usedValues, ctx::setRef);

    return proj;
  }
}
