package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.OpVisitor;
import sjtu.ipads.wtune.superopt.fragment.Proj;

class FragmentComplexity implements Complexity {
  private final int[] opCounts = new int[OperatorType.values().length + 1];

  FragmentComplexity(Op tree) {
    tree.acceptVisitor(OpVisitor.traverse(this::incrementOpCount));
  }

  FragmentComplexity(Fragment fragment) {
    this(fragment.root());
  }

  private void incrementOpCount(Op op) {
    ++opCounts[op.kind().ordinal()];
    // Treat deduplication as an operator.
    if (op.kind() == OperatorType.PROJ && ((Proj) op).isDeduplicated())
      ++opCounts[opCounts.length - 1];
  }

  @Override
  public int[] opCounts() {
    return opCounts;
  }
}
