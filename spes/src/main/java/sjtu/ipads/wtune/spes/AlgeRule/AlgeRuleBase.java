package sjtu.ipads.wtune.spes.AlgeRule;

import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;

public abstract class AlgeRuleBase {
  protected AlgeNode input;

  public abstract boolean preCondition();

  public abstract AlgeNode transformation();
}
