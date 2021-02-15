package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRefs;

public abstract class FilterNodeBase extends PlanNodeBase implements FilterNode {
  protected final ASTNode expr;
  protected List<OutputAttribute> usedAttributes;

  protected FilterNodeBase(ASTNode expr, List<OutputAttribute> usedAttributes) {
    this.expr = expr;
    this.usedAttributes = usedAttributes;
  }

  @Override
  public ASTNode expr() {
    final ASTNode copy = expr.copy();
    final List<ASTNode> nodes = collectColumnRefs(copy);

    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      final OutputAttribute usedAttr = usedAttributes.get(i);
      if (usedAttr != null) nodes.get(i).update(usedAttr.toColumnRef());
    }

    return copy;
  }

  @Override
  public List<OutputAttribute> outputAttributes() {
    return predecessors()[0].outputAttributes();
  }

  @Override
  public List<OutputAttribute> usedAttributes() {
    return usedAttributes;
  }

  @Override
  public void resolveUsedAttributes() {
    if (usedAttributes == null)
      usedAttributes = resolveUsedAttributes0(collectColumnRefs(expr), predecessors()[0]);
    else usedAttributes = resolveUsedAttributes1(usedAttributes, predecessors()[0]);
  }
}
