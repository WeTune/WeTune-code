package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.PlainFilter;
import sjtu.ipads.wtune.superopt.fragment.SubqueryFilter;

public class Canonicalization implements OperatorVisitor {
  private Operator root;

  private Canonicalization(Operator root) {
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
    final Canonicalization visitor = new Canonicalization(fragment.head());
    fragment.acceptVisitor(visitor);
    fragment.setHead(visitor.root);
    return fragment;
  }
}
