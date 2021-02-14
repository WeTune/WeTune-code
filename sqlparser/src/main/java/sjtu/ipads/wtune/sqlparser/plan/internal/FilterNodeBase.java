package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRef;

public abstract class FilterNodeBase extends PlanNodeBase implements FilterNode {
  private final ASTNode expr;
  private final List<ASTNode> columnRefs;

  protected FilterNodeBase(ASTNode expr) {
    this.expr = expr;
    this.columnRefs = collectColumnRef(expr);
  }

  @Override
  public ASTNode expr() {
    final ASTNode copy = expr.copy();

    final List<ASTNode> nodes = collectColumnRef(copy);
    final List<OutputAttribute> usedAttrs = usedAttributes0(nodes);

    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      final OutputAttribute usedAttr = usedAttrs.get(i);
      if (usedAttr != null) nodes.get(i).update(usedAttr.toColumnRef());
    }

    return copy;
  }

  @Override
  public List<OutputAttribute> outputAttributes() {
    return predecessors()[0].outputAttributes();
  }

  @Override
  public List<OutputAttribute> usedAttributes() {
    return usedAttributes0(columnRefs);
  }

  private List<OutputAttribute> usedAttributes0(List<ASTNode> nodes) {
    return listMap(predecessors()[0]::outputAttribute, nodes);
  }
}
