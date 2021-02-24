package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRefs;

public abstract class FilterNodeBase extends PlanNodeBase implements FilterNode {
  protected final ASTNode expr;
  protected List<PlanAttribute> usedAttributes;

  protected FilterNodeBase(ASTNode expr, List<PlanAttribute> usedAttributes) {
    this.expr = expr;
    this.usedAttributes = usedAttributes;
  }

  @Override
  public ASTNode expr() {
    final ASTNode copy = expr.deepCopy();
    final List<ASTNode> nodes = collectColumnRefs(copy);

    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      final PlanAttribute usedAttr = usedAttributes.get(i);
      if (usedAttr != null) nodes.get(i).update(usedAttr.toColumnRef());
    }

    return copy;
  }

  @Override
  public List<PlanAttribute> outputAttributes() {
    return predecessors()[0].outputAttributes();
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return usedAttributes;
  }

  @Override
  public void resolveUsedAttributes() {
    if (usedAttributes == null)
      usedAttributes = resolveUsedAttributes0(collectColumnRefs(expr), predecessors()[0]);
    else usedAttributes = resolveUsedAttributes1(usedAttributes, predecessors()[0]);
  }
}
