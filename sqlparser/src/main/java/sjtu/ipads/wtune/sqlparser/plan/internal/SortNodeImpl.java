package sjtu.ipads.wtune.sqlparser.plan.internal;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SortNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class SortNodeImpl extends PlanNodeBase implements SortNode {
  private final List<ASTNode> orderKeys;
  private TIntList usedAttrs;

  private SortNodeImpl(List<ASTNode> orderKeys, TIntList usedAttrs) {
    this.orderKeys = orderKeys;
    this.usedAttrs = usedAttrs;
  }

  public static SortNode build(List<ASTNode> orderKeys) {
    return new SortNodeImpl(orderKeys, null);
  }

  @Override
  public List<AttributeDef> definedAttributes() {
    return predecessors()[0].definedAttributes();
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    final List<AttributeDef> used = new ArrayList<>(usedAttrs.size());
    final List<AttributeDef> inputAttrs = predecessors()[0].definedAttributes();
    usedAttrs.forEach(it -> used.add(it == -1 ? null : inputAttrs.get(it)));
    return used;
  }

  @Override
  public void resolveUsed() {
    if (usedAttrs == null) {
      final PlanNode input = predecessors()[0];
      final List<AttributeDef> attrs = input.definedAttributes();

      final TIntList used = new TIntArrayList(orderKeys.size());
      final List<ASTNode> colRefs = gatherColumnRefs(orderKeys);
      for (ASTNode colRef : colRefs) {
        final AttributeDef resolved = input.resolveAttribute(colRef);
        used.add(resolved != null ? attrs.indexOf(resolved) : -1);
      }
      this.usedAttrs = used;
    }
  }

  @Override
  public List<ASTNode> orderKeys() {
    final List<ASTNode> keys = listMap(ASTNode::deepCopy, orderKeys);
    updateColumnRefs(gatherColumnRefs(keys), usedAttrs, predecessors()[0].definedAttributes());
    return keys;
  }

  @Override
  protected PlanNode copy0() {
    return new SortNodeImpl(orderKeys, usedAttrs);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SortNodeImpl sortNode = (SortNodeImpl) o;
    return Objects.equals(orderKeys, sortNode.orderKeys);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderKeys);
  }

  @Override
  public String toString() {
    return "Sort<%s>".formatted(orderKeys());
  }
}
