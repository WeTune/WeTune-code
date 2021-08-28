package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.superopt.fragment.FragmentUtils.bindValues;

public interface Proj extends Op {
  Symbol attrs();

  void setDeduplicated(boolean flag);

  boolean isDeduplicated();

  @Override
  default OperatorType kind() {
    return OperatorType.PROJ;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    if (node == null || node.kind() != kind()) return false;
    if (isDeduplicated() != ((ProjNode) node).isDeduplicated()) return false;

    return m.assign(attrs(), node.context().deRef(node.refs()), node.values());
  }

  @Override
  default PlanNode instantiate(Model m, PlanContext ctx) {
    final PlanNode predecessor = predecessors()[0].instantiate(m, ctx);
    final var pair = m.interpretAttrs(attrs());
    final List<Value> inValues = pair.getLeft();
    final List<Value> outValues;
    if (pair.getRight() != null) outValues = pair.getRight();
    else outValues = listMap(inValues, Value::wrapAsExprValue);

    final ProjNode proj = ProjNode.mk(ValueBag.mk(outValues));
    proj.setDeduplicated(isDeduplicated());

    proj.setContext(ctx);
    proj.setPredecessor(0, predecessor);

    ctx.registerValues(proj, proj.values());
    ctx.registerRefs(proj, proj.refs());

    final List<Value> usedValues = bindValues(inValues, predecessor);
    zipForEach(proj.refs(), usedValues, ctx::setRef);

    return proj;
  }
}
