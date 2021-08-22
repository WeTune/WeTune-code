package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

class ComplexityImpl implements Complexity {
  private final int[] opCounts = new int[OperatorType.values().length + 1];

  ComplexityImpl(Op tree) {
    tree.acceptVisitor(OpVisitor.traverse(this::incrementOpCount));
  }

  ComplexityImpl(Fragment fragment) {
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

  @Override
  public int compareTo(Complexity o) {
    return compareTo(o, true);
  }

  @Override
  public int compareTo(Complexity other, boolean preferInnerJoin) {
    final int[] opCount0 = opCounts();
    final int[] opCount1 = other.opCounts();

    final int numInput0 = opCount0[INPUT.ordinal()];
    final int numInput1 = opCount1[INPUT.ordinal()];
    if (numInput0 < numInput1) return -1;
    if (numInput0 > numInput1) return 1;

    int result = 0;

    for (int i = 0, bound = opCount0.length; i < bound; i++) {
      // LeftJoin & InnerJoin are specially handled. See below.
      if (i == LEFT_JOIN.ordinal() || i == INNER_JOIN.ordinal()) continue;

      if (result < 0 && opCount0[i] > opCount1[i]) return 0;
      if (result > 0 && opCount0[i] < opCount1[i]) return 0;
      if (opCount0[i] > opCount1[i]) result = 1;
      else if (opCount0[i] < opCount1[i]) result = -1;
    }

    if (result != 0) return result;

    final int numInnerJoin0 = opCount0[INNER_JOIN.ordinal()];
    final int numLeftJoin0 = opCount0[LEFT_JOIN.ordinal()];
    final int numInnerJoin1 = opCount1[INNER_JOIN.ordinal()];
    final int numLeftJoin1 = opCount1[LEFT_JOIN.ordinal()];
    final int numJoin0 = numInnerJoin0 + numLeftJoin0;
    final int numJoin1 = numInnerJoin1 + numLeftJoin1;

    if (numJoin0 < numJoin1) return -1;
    if (numJoin0 > numJoin1) return 1;

    return preferInnerJoin ? Integer.signum(numLeftJoin0 - numLeftJoin1) : 0;
  }
}
