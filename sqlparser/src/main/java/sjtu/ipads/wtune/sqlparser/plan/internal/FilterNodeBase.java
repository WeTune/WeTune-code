package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator.toPlan;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public abstract class FilterNodeBase extends PlanNodeBase implements FilterNode {
  protected ASTNode expr;
  protected List<AttributeDef> usedAttrs;

  protected boolean dirty = true;

  protected FilterNodeBase(ASTNode expr, List<AttributeDef> usedAttrs) {
    this.expr = expr;
    this.usedAttrs = usedAttrs;
  }

  @Override
  public ASTNode expr() {
    final ASTNode copy = expr.deepCopy();
    if (!dirty) return copy;

    dirty = true;
    updateColumnRefs(gatherColumnRefs(copy), usedAttrs);
    return this.expr = copy;
  }

  @Override
  public ASTNode rawExpr() {
    return expr;
  }

  @Override
  public List<AttributeDef> definedAttributes() {
    return predecessors()[0].definedAttributes();
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return usedAttrs;
  }

  @Override
  public void resolveUsed() {
    if (usedAttrs == null) usedAttrs = resolveUsed0(gatherColumnRefs(expr), predecessors()[0]);
    else usedAttrs = resolveUsed1(usedAttrs, predecessors()[0]);

    dirty = true;
  }

  @Override
  public List<FilterNode> breakDown() {
    if (expr.get(BINARY_OP) != BinaryOp.AND) return singletonList(this);

    resolveUsed();

    final List<ASTNode> exprs = collectExprs(expr(), new ArrayList<>());
    final List<FilterNode> filters = new ArrayList<>(exprs.size());
    for (ASTNode expr : exprs) {
      if (expr.get(BINARY_OP) == BinaryOp.IN_SUBQUERY) {
        final SubqueryFilterNode filter = SubqueryFilterNode.make(expr);
        final PlanNode subQuery = toPlan(expr.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY));
        filter.setPredecessor(1, subQuery);
        filters.add(filter);

      } else filters.add(PlainFilterNode.make(expr));
    }

    return filters;
  }

  private List<ASTNode> collectExprs(ASTNode node, List<ASTNode> exprs) {
    if (node.get(BINARY_OP) == BinaryOp.AND) {
      collectExprs(node.get(BINARY_RIGHT), exprs);
      collectExprs(node.get(BINARY_LEFT), exprs);
    } else exprs.add(node);
    return exprs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FilterNodeBase that = (FilterNodeBase) o;
    return expr.toString().equalsIgnoreCase(that.expr.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getClass(), expr.toString().toLowerCase());
  }
}
