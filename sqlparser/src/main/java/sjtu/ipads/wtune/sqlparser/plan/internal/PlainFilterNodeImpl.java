package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class PlainFilterNodeImpl extends FilterNodeBase implements PlainFilterNode {
  private Set<AttributeDef> fixedValueAttributes;

  private PlainFilterNodeImpl(ASTNode expr, List<AttributeDef> usedAttrs) {
    super(expr, usedAttrs);
  }

  public static PlainFilterNode build(ASTNode expr) {
    return new PlainFilterNodeImpl(expr, null);
  }

  public static PlainFilterNode build(ASTNode expr, List<AttributeDef> usedAttrs) {
    return new PlainFilterNodeImpl(expr, usedAttrs);
  }

  @Override
  protected PlanNode copy0() {
    return new PlainFilterNodeImpl(expr, usedAttrs);
  }

  @Override
  public Set<AttributeDef> fixedValueAttributes() {
    if (!dirty && fixedValueAttributes != null) return fixedValueAttributes;

    final List<ASTNode> colRefs = gatherColumnRefs(expr());
    final List<AttributeDef> used = usedAttributes();
    assert colRefs.size() == used.size();

    final Set<AttributeDef> ret = newIdentitySet(colRefs.size());
    for (int i = 0; i < colRefs.size(); i++) if (isFixedValue(colRefs.get(i))) ret.add(used.get(i));

    return ret;
  }

  private static boolean isFixedValue(ASTNode colRef) {
    final ASTNode parent = colRef.parent();
    final BinaryOp op = parent.get(BINARY_OP);
    if (op != BinaryOp.EQUAL && op != BinaryOp.IS) return false;
    ASTNode node = parent.parent();
    while (EXPR.isInstance(node)) {
      if (node.get(BINARY_OP) != BinaryOp.AND) return false;
      node = node.parent();
    }
    return true;
  }

  @Override
  public String toString() {
    return "PlainFilter<%s>".formatted(expr());
  }
}
