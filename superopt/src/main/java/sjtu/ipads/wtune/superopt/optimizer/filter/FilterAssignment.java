package sjtu.ipads.wtune.superopt.optimizer.filter;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Filter;

import java.util.Comparator;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;

public record FilterAssignment(Filter op, List<FilterNode> used) {
  public FilterNode assignment() {
    assert !used.isEmpty();

    used.sort(Comparator.comparing(FilterNode::toString));

    if (used.size() == 1) {
      final FilterNode node = (FilterNode) Iterables.getOnlyElement(used).copy();

      if (node.type() == OperatorType.SubqueryFilter)
        node.setPredecessor(1, PlanNode.copyOnTree(node.predecessors()[1]));
      return node;

    } else return used.stream().reduce(FilterAssignment::combineFilters).get();
  }

  private static FilterNode combineFilters(FilterNode f0, FilterNode f1) {
    final ASTNode binary = ASTNode.expr(ExprKind.BINARY);
    binary.set(BINARY_LEFT, f0.expr()); // no need to copy(), expr() always returns a fresh AST
    binary.set(BINARY_RIGHT, f1.expr());
    binary.set(BINARY_OP, BinaryOp.AND);

    return PlainFilterNode.make(binary, listJoin(f0.usedAttributes(), f1.usedAttributes()));
  }
}
