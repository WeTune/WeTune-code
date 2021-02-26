package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private final List<ASTNode> selectItems;
  private final List<PlanAttribute> defined;
  private List<PlanAttribute> used;

  private ProjNodeImpl(List<PlanAttribute> defined, List<PlanAttribute> used) {
    this.selectItems = listMap(PlanAttribute::toSelectItem, defined);
    this.defined = defined;
    this.used = used;
    bindAttributes(defined, this);
  }

  public static ProjNode build(List<PlanAttribute> definedAttrs) {
    return new ProjNodeImpl(definedAttrs, null);
  }

  public static ProjNode build(List<PlanAttribute> definedAttrs, List<PlanAttribute> usedAttrs) {
    if (definedAttrs == null) definedAttrs = usedAttrs;
    return new ProjNodeImpl(listMap(PlanAttribute::copy, definedAttrs), usedAttrs);
  }

  @Override
  public List<ASTNode> selectItems() {
    final List<ASTNode> copy = listMap(ASTNode::deepCopy, selectItems);
    updateColumnRefs(gatherColumnRefs(copy), used);
    return copy;
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return used;
  }

  @Override
  public List<PlanAttribute> definedAttributes() {
    return defined;
  }

  @Override
  public void resolveUsedTree() {
    final PlanNode input = predecessors()[0];
    if (used != null) used = resolveUsed1(used, input);
    else used = resolveUsed0(gatherColumnRefs(listMap(PlanAttribute::expr, defined)), input);
  }

  @Override
  protected PlanNode copy0() {
    return build(defined, used);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjNodeImpl projNode = (ProjNodeImpl) o;
    return Objects.equals(selectItems, projNode.selectItems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(selectItems);
  }
}
