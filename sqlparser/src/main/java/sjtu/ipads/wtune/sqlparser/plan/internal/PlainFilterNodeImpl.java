package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class PlainFilterNodeImpl extends FilterNodeBase implements PlainFilterNode {
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
    final List<ASTNode> colRefs = gatherColumnRefs(expr);
    final List<AttributeDef> used = usedAttributes();
    final Set<AttributeDef> ret = Collections.newSetFromMap(new IdentityHashMap<>());
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
