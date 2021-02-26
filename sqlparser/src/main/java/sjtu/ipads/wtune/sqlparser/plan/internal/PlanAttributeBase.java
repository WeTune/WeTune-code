package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Objects;

import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public abstract class PlanAttributeBase implements PlanAttribute {
  private final String name;
  private final String qualification;
  private PlanNode origin;

  public PlanAttributeBase(String qualification, String name) {
    this.qualification = simpleName(qualification);
    this.name = simpleName(name);
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
  public PlanNode origin() {
    return origin;
  }

  @Override
  public void setOrigin(PlanNode origin) {
    this.origin = origin;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DerivedPlanAttribute that = (DerivedPlanAttribute) o;
    return Objects.equals(name(), that.name()) && PlanNode.equalsTree(origin(), that.origin());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name()) * 31 + PlanNode.hashCodeTree(origin());
  }
}
