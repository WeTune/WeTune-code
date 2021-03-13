package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.common.utils.TypedTreeNode;
import sjtu.ipads.wtune.common.utils.TypedTreeVisitor;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;

import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.InnerJoin;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LeftJoin;

public class CostEstimator {
  public static int compareCost(PlanNode o1, PlanNode o2) {
    return compareCost(computeComplexity(o1), computeComplexity(o2), true);
  }

  public static int compareCost(Fragment g0, Fragment g1) {
    return compareCost(computeComplexity(g0), computeComplexity(g1), true);
  }

  private static int compareCost(int[] opCount0, int[] opCount1, boolean preferInner) {
    int result = 0;

    for (int i = 0, bound = opCount0.length; i < bound; i++) {
      if (i == LeftJoin.ordinal() || i == InnerJoin.ordinal()) continue;

      if (result < 0 && opCount0[i] > opCount1[i]) return 0;
      if (result > 0 && opCount0[i] < opCount1[i]) return 0;
      if (opCount0[i] > opCount1[i]) result = 1;
      else if (opCount0[i] < opCount1[i]) result = -1;
    }

    if (result != 0) return result;

    final int numInnerJoin0 = opCount0[InnerJoin.ordinal()];
    final int numLeftJoin0 = opCount0[LeftJoin.ordinal()];
    final int numInnerJoin1 = opCount1[InnerJoin.ordinal()];
    final int numLeftJoin1 = opCount1[LeftJoin.ordinal()];
    final int numJoin0 = numInnerJoin0 + numLeftJoin0, numJoin1 = numInnerJoin1 + numLeftJoin1;

    if (numJoin0 < numJoin1) return -1;
    if (numJoin0 > numJoin1) return 1;
    // if `preferInner`, consider inner join is always better than left join
    final int leftJoinDiff = Integer.signum(numLeftJoin0 - numLeftJoin1);
    return preferInner ? leftJoinDiff : 0;
  }

  public static int[] computeComplexity(Fragment g0) {
    final OperatorCounter counter = new OperatorCounter();
    g0.head().accept(counter);
    return counter.counters;
  }

  public static int[] computeComplexity(PlanNode p) {
    final OperatorCounter counter = new OperatorCounter();
    p.accept(counter);
    return counter.counters;
  }

  private static class OperatorCounter
      implements TypedTreeVisitor<OperatorType, TypedTreeNode<OperatorType>> {
    private final int[] counters = new int[OperatorType.values().length];

    @Override
    public boolean on(TypedTreeNode<OperatorType> n) {
      counters[n.type().ordinal()]++;
      return true;
    }

    @Override
    public void off(TypedTreeNode<OperatorType> n) {}
  }
}
