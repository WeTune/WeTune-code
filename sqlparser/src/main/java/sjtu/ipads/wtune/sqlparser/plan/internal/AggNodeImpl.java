package sjtu.ipads.wtune.sqlparser.plan.internal;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func2;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.AGGREGATE;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class AggNodeImpl extends PlanNodeBase implements AggNode {
  private final List<ASTNode> groupKeys;
  private final List<ASTNode> selectItems;

  private final List<AttributeDef> definedAttrs;

  private TIntList keyUsedAttrs;
  private TIntList aggUsedAttrs;

  private AggNodeImpl(String qualification, List<ASTNode> selectItems, List<ASTNode> groupKeys) {
    this.groupKeys = groupKeys;
    this.selectItems = selectItems;
    this.definedAttrs = listMap(func2(this::makeAttribute).bind0(qualification), selectItems);
    bindAttributes(definedAttrs, this);
  }

  private AggNodeImpl(
      List<AttributeDef> definedAttrs,
      List<ASTNode> groupKeys,
      TIntList keyUsedAttrs,
      TIntList aggUsedAttrs) {
    this.definedAttrs = listMap(AttributeDef::copy, definedAttrs);
    this.selectItems = listMap(AttributeDef::toSelectItem, definedAttrs);
    this.groupKeys = groupKeys;
    this.keyUsedAttrs = keyUsedAttrs;
    this.aggUsedAttrs = aggUsedAttrs;
    bindAttributes(this.definedAttrs, this);
  }

  public static AggNode build(String qualification, List<ASTNode> aggs, List<ASTNode> groupKeys) {
    return new AggNodeImpl(qualification, aggs, groupKeys);
  }

  @Override
  public List<ASTNode> groupKeys() {
    if (groupKeys == null) return null;
    final List<ASTNode> keys = listMap(ASTNode::deepCopy, groupKeys);
    updateColumnRefs(gatherColumnRefs(keys), keyUsedAttrs, predecessors()[0].definedAttributes());
    return keys;
  }

  @Override
  public List<ASTNode> aggregations() {
    final List<ASTNode> aggs = listMap(ASTNode::deepCopy, selectItems);
    final List<ASTNode> refs = gatherColumnRefs(aggs);
    final List<AttributeDef> inputAttrs = predecessors()[0].definedAttributes();
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final int attrIdx = aggUsedAttrs.get(i);
      if (attrIdx != -1) refs.get(i).update(inputAttrs.get(attrIdx).upstream().toColumnRef());
    }

    final ProjNode proj = (ProjNode) predecessors()[0];
    if (!proj.isForcedUnique())
      for (ASTNode agg : aggs) {
        final ASTNode expr = agg.get(SELECT_ITEM_EXPR);
        if (AGGREGATE.isInstance(expr)) expr.set(AGGREGATE_DISTINCT, false);
      }

    return aggs;
  }

  @Override
  public List<AttributeDef> definedAttributes() {
    return definedAttrs;
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    final List<AttributeDef> used = new ArrayList<>(aggUsedAttrs.size());
    final List<AttributeDef> inputAttrs = predecessors()[0].definedAttributes();
    aggUsedAttrs.forEach(it -> used.add(inputAttrs.get(it)));
    if (keyUsedAttrs != null) keyUsedAttrs.forEach(it -> used.add(inputAttrs.get(it)));
    return used;
  }

  @Override
  public void resolveUsed() {
    // `input` must contains all attributes used in groupKeys and groupItems,
    // and its output relation must keep unchanged even after substitution.
    // Thus, to handle attribute displace, the used attribute are recorded by index.
    //
    // Example:
    // SQL: SELECT COUNT(b.id) FROM a JOIN b ON a.ref = b.id
    // Plan: Agg<[0]>(Proj<b.id>(InnerJoin<a.ref=b.id>(Input<a>, Input<b>)))
    // Plan_opt: Agg<[0]>(Proj<a.ref>(Input<a>))
    // SQL_opt: SELECT COUNT(a.ref) FROM a JOIN b ON a.ref = b.id
    if (aggUsedAttrs == null) aggUsedAttrs = resolveUsedAttributes0(selectItems);
    if (groupKeys != null && keyUsedAttrs == null) keyUsedAttrs = resolveUsedAttributes0(groupKeys);
  }

  @Override
  protected PlanNode copy0() {
    return new AggNodeImpl(definedAttrs, groupKeys, keyUsedAttrs, aggUsedAttrs);
  }

  @Override
  public void setPredecessor(int idx, PlanNode op) {
    if (op != null && !(op instanceof ProjNode))
      throw new IllegalArgumentException("AggNode should only be preceded by ProjNode");

    super.setPredecessor(idx, op);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AggNodeImpl aggNode = (AggNodeImpl) o;
    return Objects.equals(groupKeys, aggNode.groupKeys)
        && Objects.equals(selectItems, aggNode.selectItems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupKeys, selectItems);
  }

  @Override
  public String toString() {
    return "Agg<%s %s>".formatted(groupKeys(), aggregations());
  }

  private TIntList resolveUsedAttributes0(List<ASTNode> nodes) {
    final PlanNode input = predecessors()[0];
    final List<AttributeDef> inputAttrs = input.definedAttributes();
    assert input instanceof ProjNode;

    final TIntList usedAttrs = new TIntArrayList(nodes.size());
    for (ASTNode node : nodes)
      for (ASTNode colRef : gatherColumnRefs(node)) {
        final AttributeDef resolved = input.resolveAttribute(colRef);
        usedAttrs.add(resolved != null ? inputAttrs.indexOf(resolved) : -1);
      }
    return usedAttrs;
  }
}
