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
  private final List<AttributeDef> defined;
  private List<ASTNode> selectItems;
  private List<AttributeDef> used;

  private boolean isForcedUnique;
  private boolean isWildcard;

  private boolean dirty = true;

  private ProjNodeImpl(String qualification, List<ASTNode> selectItems) {
    this.defined = listMap(func2(this::makeAttribute).bind0(qualification), selectItems);
    this.selectItems = listMap(AttributeDef::toSelectItem, defined);
    bindAttributes(defined, this);
  }

  private ProjNodeImpl(List<AttributeDef> defined) {
    this.defined = listMap(this::rectifyAttribute, defined);
    this.selectItems = listMap(AttributeDef::toSelectItem, defined);
    bindAttributes(this.defined, this);
  }

  private AttributeDef rectifyAttribute(AttributeDef def) {
    if (def instanceof DerivedAttributeDef) return def.copy();
    else return makeAttribute(def.qualification(), def.toSelectItem());
  }

  public static ProjNode build(String qualification, List<ASTNode> selectItems) {
    return new ProjNodeImpl(qualification, selectItems);
  }

  public static ProjNode build(List<AttributeDef> definedAttrs) {
    return new ProjNodeImpl(definedAttrs);
  }

  @Override
  public boolean isForcedUnique() {
    return isForcedUnique;
  }

  @Override
  public void setForcedUnique(boolean forcedUnique) {
    isForcedUnique = forcedUnique;
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
    if (!dirty) return copy;

    dirty = false;
    updateColumnRefs(gatherColumnRefs(copy), used);
    return this.selectItems = copy;
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

    final PlanNode succ = successor();
    if ((succ == null && definedAttributes().equals(predecessors()[0].definedAttributes()))
        || (succ != null && definedAttributes().containsAll(predecessors()[0].definedAttributes())))
      isWildcard = true;

    dirty = true;
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

  @Override
  public String toString() {
    return "Proj<%s>".formatted(selectItems());
  }
}
