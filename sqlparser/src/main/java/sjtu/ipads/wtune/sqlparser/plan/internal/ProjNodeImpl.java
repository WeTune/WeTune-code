package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private final List<PlanAttribute> projections;

  private ProjNodeImpl(List<PlanAttribute> projs) {
    this.projections = projs;
  }

  public static ProjNodeImpl build(List<PlanAttribute> projs) {
    return new ProjNodeImpl(projs);
  }

  @Override
  public List<ASTNode> selectItems() {
    return listMap(PlanAttribute::toSelectItem, projections);
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return listFlatMap(PlanAttribute::used, projections);
  }

  @Override
  public List<PlanAttribute> outputAttributes() {
    return projections;
  }

  @Override
  public void resolveUsedAttributes() {
    final PlanNode input = predecessors()[0];

    for (PlanAttribute proj : projections) {
      final List<PlanAttribute> used = proj.used();
      if (used == null)
        if (WILDCARD.isInstance(proj.expr())) {
          final String[] refName = proj.referenceName();
          final PlanAttribute ref = input.resolveAttribute(refName[0], refName[1]);
          proj.setUsed(ref != null ? singletonList(ref) : emptyList());

        } else proj.setUsed(resolveUsedAttributes0(gatherColumnRefs(proj.expr()), input));
      else proj.setUsed(resolveUsedAttributes1(proj.used(), input));
    }
  }

  @Override
  protected PlanNode copy0() {
    return new ProjNodeImpl(projections);
  }
}
