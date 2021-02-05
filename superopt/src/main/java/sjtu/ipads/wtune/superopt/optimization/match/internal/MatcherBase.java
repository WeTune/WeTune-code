package sjtu.ipads.wtune.superopt.optimization.match.internal;

import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.Proj;
import sjtu.ipads.wtune.superopt.optimization.match.Matcher;

public abstract class MatcherBase implements Matcher {
  protected final PlanNode planNode;

  protected MatcherBase(PlanNode planNode) {
    this.planNode = planNode;
  }

  static Matcher matcher(PlanNode node) {
    if (node instanceof Proj) return new ProjMatcher((Proj) node);
    if (node instanceof Input) return new InputMatcher((Input) node);
    return null;
  }

  @Override
  public <T extends PlanNode> T planNode() {
    return (T) planNode;
  }
}
