package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func;
import static sjtu.ipads.wtune.common.utils.FuncUtils.func2;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

public class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private final List<AttributeDef> defined;
  private List<ASTNode> selectItems;
  private List<AttributeDef> used;

  private boolean isForcedUnique;
  private boolean isWildcard;

  private boolean dirty = true;

  private ProjNodeImpl(String qualification, List<ASTNode> selectItems) {
    selectItems = listMap(func(ASTNode::deepCopy).andThen(ASTContext::unmanage), selectItems);

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
    return isWildcard
        && successor() != null
        && (successor().type().isJoin() || successor().type() == OperatorType.SubqueryFilter);
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
    this.selectItems = copy;
    return new ArrayList<>(copy);
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
    used = listFlatMap(AttributeDef::references, defined);
    //    final PlanNode input = predecessors()[0];
    //    if (used != null) used = resolveUsed1(used, input);
    //    else used = resolveUsed0(gatherColumnRefs(selectItems), input);

    final PlanNode succ = successor();
    final List<AttributeDef> definedAttrs = definedAttributes();
    final List<AttributeDef> inputAttrs = predecessors()[0].definedAttributes();

    if (!isWildcard && succ != null && definedAttrs.containsAll(inputAttrs)) isWildcard = true;

    dirty = true;
  }

  @Override
  protected PlanNode copy0() {
    final ProjNodeImpl copy = new ProjNodeImpl(defined);
    copy.setForcedUnique(isForcedUnique);
    copy.setWildcard(isWildcard);
    return copy;
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
    return "Proj%s<%s>".formatted(isForcedUnique ? "'" : "", selectItems());
  }
}
