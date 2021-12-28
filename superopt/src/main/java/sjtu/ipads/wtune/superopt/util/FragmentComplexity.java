package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.OpVisitor;
import sjtu.ipads.wtune.superopt.fragment.Proj;

import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PROJ;

class FragmentComplexity implements Complexity {
  private final int[] opCounts = new int[OperatorType.values().length + 2];

  FragmentComplexity(Op tree) {
    tree.acceptVisitor(OpVisitor.traverse(this::incrementOpCount));
    final int projCount = opCounts[PROJ.ordinal()];
    opCounts[opCounts.length - 1] = tree.kind() == PROJ ? projCount - 1 : projCount;
  }

  FragmentComplexity(Fragment fragment) {
    this(fragment.root());
  }

  private void incrementOpCount(Op op) {
    ++opCounts[op.kind().ordinal()];
    // Treat deduplication as an operator.
    if (op.kind() == PROJ && ((Proj) op).isDeduplicated()) ++opCounts[opCounts.length - 1];
  }

  @Override
  public int[] opCounts() {
    return opCounts;
  }
}
