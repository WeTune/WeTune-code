package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func2;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private final List<ASTNode> selectItems;
  private final List<AttributeDef> defined;
  private List<AttributeDef> used;

  private boolean isWildcard;

  private ProjNodeImpl(String qualification, List<ASTNode> selectItems) {
    this.selectItems = selectItems;
    this.defined = listMap(func2(this::makeAttribute).bind0(qualification), selectItems);
    bindAttributes(defined, this);
  }

  private ProjNodeImpl(List<AttributeDef> defined) {
    this.selectItems = listMap(AttributeDef::toSelectItem, defined);
    this.defined = listMap(AttributeDef::copy, defined);
    bindAttributes(this.defined, this);
  }

  public static ProjNode build(String qualification, List<ASTNode> selectItems) {
    return new ProjNodeImpl(qualification, selectItems);
  }

  public static ProjNode build(List<AttributeDef> definedAttrs) {
    return new ProjNodeImpl(definedAttrs);
  }

  @Override
  public boolean isWildcard() {
    return isWildcard;
  }

  @Override
  public void setWildcard(boolean wildcard) {
    isWildcard = wildcard;
  }

  @Override
  public List<ASTNode> selectItems() {
    final List<ASTNode> copy = listMap(ASTNode::deepCopy, selectItems);
    updateColumnRefs(gatherColumnRefs(copy), used);
    return copy;
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return used;
  }

  @Override
  public List<AttributeDef> definedAttributes() {
    return defined;
  }

  @Override
  public void resolveUsed() {
    final PlanNode input = predecessors()[0];
    if (used != null) used = resolveUsed1(used, input);
    else used = resolveUsed0(gatherColumnRefs(selectItems), input);

    if (used.equals(predecessors()[0].definedAttributes())) isWildcard = true;
  }

  @Override
  protected PlanNode copy0() {
    return new ProjNodeImpl(defined);
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
