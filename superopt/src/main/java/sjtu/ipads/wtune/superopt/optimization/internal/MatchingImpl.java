package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimization.Match;

public class MatchingImpl implements Match {
  private final PlanNode matchPoint;
  private final Snapshot interpretation;

  private MatchingImpl(PlanNode matchPoint, Snapshot interpretation) {
    this.matchPoint = matchPoint;
    this.interpretation = interpretation;
  }

  public static Match build(PlanNode matchPoint, Snapshot interpretation) {
    return new MatchingImpl(matchPoint, interpretation);
  }

  @Override
  public PlanNode matchPoint() {
    return matchPoint;
  }

  @Override
  public Snapshot interpretation() {
    return interpretation;
  }
}
