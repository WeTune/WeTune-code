package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;

import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public abstract class FilterNodeBase extends PlanNodeBase implements FilterNode {
  protected final ASTNode expr;
  protected List<PlanAttribute> usedAttrs;

  protected FilterNodeBase(ASTNode expr, List<PlanAttribute> usedAttrs) {
    this.expr = expr;
    this.usedAttrs = usedAttrs;
  }

  @Override
  public ASTNode expr() {
    final ASTNode copy = expr.deepCopy();
    updateColumnRefs(gatherColumnRefs(copy), usedAttrs);
    return copy;
  }

  @Override
  public List<PlanAttribute> definedAttributes() {
    return predecessors()[0].definedAttributes();
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return usedAttrs;
  }

  @Override
  public void resolveUsedTree() {
    if (usedAttrs == null) usedAttrs = resolveUsed0(gatherColumnRefs(expr), predecessors()[0]);
    else usedAttrs = resolveUsed1(usedAttrs, predecessors()[0]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FilterNodeBase that = (FilterNodeBase) o;
    return Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getClass(), expr);
  }
}
