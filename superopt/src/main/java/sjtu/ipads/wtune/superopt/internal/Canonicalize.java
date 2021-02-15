package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.fragment.*;

public class Canonicalize implements OperatorVisitor {
  private Operator root;

  private Canonicalize(Operator root) {
    this.root = root;
  }

  @Override
  public boolean enterPlainFilter(PlainFilter op) {
    final Operator succ = op.successor();

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

  public static Fragment canonicalize(Fragment fragment) {
    final Canonicalize visitor = new Canonicalize(fragment.head());
    fragment.acceptVisitor(visitor);
    fragment.setHead(visitor.root);
    return fragment;
  }
}
