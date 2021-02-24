package sjtu.ipads.wtune.sqlparser.plan.internal;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class AggNodeImpl extends PlanNodeBase implements AggNode {
  private final List<PlanAttribute> attrs;
  private final List<ASTNode> groupKeys;
  private final List<ASTNode> aggregations;

  private TIntList keyUsedAttrs;
  private TIntList aggUsedAttrs;

  private AggNodeImpl(
      List<PlanAttribute> attrs,
      List<ASTNode> groupKeys,
      TIntList keyUsedAttrs,
      TIntList aggUsedAttrs) {
    this.attrs = attrs;
    this.groupKeys = groupKeys;
    this.aggregations = listMap(PlanAttribute::expr, attrs);
    this.keyUsedAttrs = keyUsedAttrs;
    this.aggUsedAttrs = aggUsedAttrs;
  }

  public static AggNode build(List<PlanAttribute> aggs, List<ASTNode> groupKeys) {
    return new AggNodeImpl(aggs, groupKeys, null, null);
  }

  @Override
  public List<ASTNode> groupKeys() {
    if (groupKeys == null) return null;
    final List<ASTNode> keys = listMap(ASTNode::deepCopy, groupKeys);
    return updateColumnRefs(gatherColumnRefs(keys), keyUsedAttrs);
  }

  @Override
  public List<ASTNode> aggregations() {
    final List<ASTNode> aggs = listMap(ASTNode::deepCopy, aggregations);
    return updateColumnRefs(gatherColumnRefs(aggs), aggUsedAttrs);
  }

  @Override
  public List<PlanAttribute> outputAttributes() {
    return attrs;
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    final List<PlanAttribute> used = new ArrayList<>(aggUsedAttrs.size() + keyUsedAttrs.size());
    final List<PlanAttribute> inputAttrs = predecessors()[0].outputAttributes();
    keyUsedAttrs.forEach(it -> used.add(inputAttrs.get(it)));
    aggUsedAttrs.forEach(it -> used.add(inputAttrs.get(it)));
    return used;
  }

  @Override
  public void resolveUsedAttributes() {
    // `input` must contains all attributes used in groupKeys and groupItems,
    // and its output relation must keep unchanged even after substitution.
    // Thus, to handle attribute displace, the used attribute are recorded by index.
    //
    // Example:
    // SQL: SELECT COUNT(b.id) FROM a JOIN b ON a.ref = b.id
    // Plan: Agg<[0]>(Proj<b.id>(InnerJoin<a.ref=b.id>(Input<a>, Input<b>)))
    // Plan_opt: Agg<[0]>(Proj<a.ref>(Input<a>))
    // SQL_opt: SELECT COUNT(a.ref) FROM a JOIN b ON a.ref = b.id
    if (aggUsedAttrs == null) aggUsedAttrs = resolveUsedAttributes0(aggregations);
    if (groupKeys != null && keyUsedAttrs == null) keyUsedAttrs = resolveUsedAttributes0(groupKeys);
  }

  @Override
  public void setPredecessor(int idx, PlanNode op) {
    if (op != null && !(op instanceof ProjNode))
      throw new IllegalArgumentException("AggNode should only be preceded by ProjNode");

    super.setPredecessor(idx, op);
  }

  private TIntList resolveUsedAttributes0(List<ASTNode> nodes) {
    final PlanNode input = predecessors()[0];
    final List<PlanAttribute> inputAttrs = input.outputAttributes();
    assert input instanceof ProjNode;

    final TIntList usedAttrs = new TIntArrayList(nodes.size());
    for (ASTNode node : nodes)
      for (ASTNode colRef : gatherColumnRefs(node)) {
        final PlanAttribute resolved = input.resolveAttribute(colRef);
        usedAttrs.add(resolved != null ? inputAttrs.indexOf(resolved) : -1);
      }
    return usedAttrs;
  }

  private List<ASTNode> updateColumnRefs(List<ASTNode> colRefs, TIntList usedAttrs) {
    final List<PlanAttribute> inputAttrs = predecessors()[0].outputAttributes();
    for (int i = 0, bound = colRefs.size(); i < bound; i++) {
      final int attrIdx = usedAttrs.get(i);
      if (attrIdx != -1) colRefs.get(i).update(inputAttrs.get(attrIdx).toColumnRef());
    }
    return colRefs;
  }

  @Override
  protected PlanNode copy0() {
    return new AggNodeImpl(attrs, groupKeys, keyUsedAttrs, aggUsedAttrs);
  }
}
