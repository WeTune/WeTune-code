package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.plan.*;

public class Canonicalize implements PlanVisitor {
  private PlanNode root;

  private Canonicalize(PlanNode root) {
    this.root = root;
  }

  @Override
  public boolean enterPlainFilter(PlainFilter op) {
    final PlanNode succ = op.successor();

    if (succ instanceof SubqueryFilter && succ.predecessors()[0] == op) {
      if (succ == root) {
        root = op;
        op.setSuccessor(null);
      } else succ.successor().replacePredecessor(succ, op);

      succ.setPredecessor(0, op.predecessors()[0]);
      op.setPredecessor(0, succ);

      op.acceptVisitor(this);
      return false;
    }

    return true;
  }

  public static Plan canonicalize(Plan plan) {
    final Canonicalize visitor = new Canonicalize(plan.head());
    plan.acceptVisitor(visitor);
    plan.setHead(visitor.root);
    return plan;
  }
}
