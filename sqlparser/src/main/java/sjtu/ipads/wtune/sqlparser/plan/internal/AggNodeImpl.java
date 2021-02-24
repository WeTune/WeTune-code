package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRefs;

public class AggNodeImpl extends PlanNodeBase implements AggNode {
  private final List<PlanAttribute> aggregations;
  private final List<ASTNode> groupKeys;
  private List<PlanAttribute> keyUsedAttrs;

  private AggNodeImpl(
      List<PlanAttribute> aggregations, List<ASTNode> groupKeys, List<PlanAttribute> keyUsedAttrs) {
    this.aggregations = aggregations;
    this.groupKeys = groupKeys;
    this.keyUsedAttrs = keyUsedAttrs;
  }

  public static AggNode build(List<PlanAttribute> aggs, List<ASTNode> groupKeys) {
    return new AggNodeImpl(aggs, groupKeys, null);
  }

  @Override
  public List<ASTNode> groupKeys() {
    if (groupKeys == null) return null;
    final List<ASTNode> keys = listMap(ASTNode::deepCopy, groupKeys);
    final List<ASTNode> refs = collectColumnRefs(keys);
    for (int i = 0, bound = refs.size(); i < bound; i++)
      refs.get(i).update(keyUsedAttrs.get(i).toColumnRef());
    return keys;
  }

  @Override
  public List<ASTNode> aggregations() {
    return listMap(PlanAttribute::toSelectItem, aggregations);
  }

  @Override
  public List<PlanAttribute> outputAttributes() {
    return aggregations;
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return listFlatMap(PlanAttribute::used, aggregations);
  }

  @Override
  public void resolveUsedAttributes() {
    final PlanNode input = predecessors()[0];

    for (PlanAttribute agg : aggregations) {
      final List<PlanAttribute> resolved = agg.used();
      if (resolved != null) agg.setUsed(resolveUsedAttributes1(resolved, input));
      else agg.setUsed(resolveUsedAttributes0(collectColumnRefs(agg.expr()), input));
    }

    if (groupKeys != null)
      if (keyUsedAttrs != null) keyUsedAttrs = resolveUsedAttributes1(keyUsedAttrs, input);
      else keyUsedAttrs = resolveUsedAttributes0(collectColumnRefs(groupKeys), input);
  }

  @Override
  protected PlanNode copy0() {
    return new AggNodeImpl(aggregations, groupKeys, keyUsedAttrs);
  }
}
