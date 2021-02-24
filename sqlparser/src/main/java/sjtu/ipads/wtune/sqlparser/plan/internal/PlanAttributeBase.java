package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;

public abstract class PlanAttributeBase implements PlanAttribute {
  private final String name;
  private final String qualification;

  public PlanAttributeBase(String qualification, String name) {
    this.qualification = qualification;
    this.name = name;
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
  public boolean refEquals(PlanAttribute other) {
    if (this == other) return true;

    final PlanAttribute thisRef = this.reference(true), otherRef = other.reference(true);
    return thisRef != null && thisRef == otherRef;
  }
}
