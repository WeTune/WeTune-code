package sjtu.ipads.wtune.superopt.plan;

public interface Join extends PlanNode {
  Placeholder leftFields();

  Placeholder rightFields();
}
