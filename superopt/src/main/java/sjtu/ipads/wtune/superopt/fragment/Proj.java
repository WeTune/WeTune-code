package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.safeGet;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.bindValues;

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
    final List<Value> inValues = m.interpretInAttrs(attrs());
    final List<List<Value>> outCandidates = m.interpretOutAttrs(attrs());
    List<Value> outValues = safeGet(outCandidates, 0);

    if (m.isAssignmentTrusted(attrs()))
      assert outValues != null; // Only reachable when called from PlanTranslator.
    else {
      if (outValues != null) outValues = listMap(outValues, Value::renewRefs);
      else outValues = listMap(inValues, Value::wrapAsExprValue);
    }

    final ProjNode proj = ProjNode.mk(ValueBag.mk(outValues));
    proj.setDeduplicated(isDeduplicated());

    proj.setContext(ctx);
    proj.setPredecessor(0, predecessor);

    ctx.registerValues(proj, proj.values());
    ctx.registerRefs(proj, proj.refs());

    final List<Value> usedValues = bindValues(inValues, predecessor, m.planContext());
    zipForEach(proj.refs(), usedValues, ctx::setRef);

    return proj;
  }
}
