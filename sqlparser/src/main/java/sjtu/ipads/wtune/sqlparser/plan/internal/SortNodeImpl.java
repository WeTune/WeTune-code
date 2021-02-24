package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SortNode;
import sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class SortNodeImpl extends PlanNodeBase implements SortNode {
  private final List<ASTNode> orderKeys;
  private List<PlanAttribute> usedAttrs;

  private SortNodeImpl(List<ASTNode> orderKeys, List<PlanAttribute> usedAttrs) {
    this.orderKeys = orderKeys;
    this.usedAttrs = usedAttrs;
  }

  public static SortNode build(List<ASTNode> orderKeys) {
    return new SortNodeImpl(orderKeys, null);
  }

  @Override
  public List<PlanAttribute> outputAttributes() {
    return predecessors()[0].outputAttributes();
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return usedAttrs;
  }

  @Override
  public void resolveUsedAttributes() {
    final PlanNode input0 = predecessors()[0], input1 = predecessors()[0].predecessors()[0];
    if (usedAttrs != null) {
      final var iter = usedAttrs.listIterator();
      while (iter.hasNext()) {
        final PlanAttribute attr = iter.next();
        PlanAttribute resolved = input0.resolveAttribute(attr);
        iter.set(resolved != null ? resolved : input1.resolveAttribute(attr));
      }
    } else {
      final List<ASTNode> colRefs = gatherColumnRefs(orderKeys);
      final List<PlanAttribute> usedAttrs = new ArrayList<>(colRefs.size());
      for (ASTNode colRef : colRefs) {
        final PlanAttribute resolved = input0.resolveAttribute(colRef);
        usedAttrs.add(resolved != null ? resolved : input1.resolveAttribute(colRef));
      }
      this.usedAttrs = usedAttrs;
    }
  }

  @Override
  public List<ASTNode> orderKeys() {
    final List<ASTNode> keys = listMap(ASTNode::deepCopy, orderKeys);
    updateColumnRefs(gatherColumnRefs(keys), usedAttrs);
    return keys;
  }

  @Override
  protected PlanNode copy0() {
    return new SortNodeImpl(orderKeys, usedAttrs);
  }
}
