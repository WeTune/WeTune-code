package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public abstract class OutputAttributeBase implements OutputAttribute {
  private final PlanNode owner;
  private final String name;
  private final String qualification;

  public OutputAttributeBase(PlanNode owner, String qualification, String name) {
    this.qualification = qualification;
    this.name = name;
    this.owner = owner;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public PlanNode owner() {
    return owner;
  }
}
