package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.relational.AggFuncs;
import sjtu.ipads.wtune.superopt.relational.GroupKeys;

public class AggImpl extends BaseOperator implements Agg {
  private final Abstraction<GroupKeys> groupKeys = Abstraction.create(this, "groupKeys-" + id());
  private final Abstraction<AggFuncs> aggFuncs = Abstraction.create(this, "aggFuncs-" + id());

  public AggImpl() {}

  public static AggImpl create() {
    return new AggImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterAgg(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveAgg(this);
  }

  @Override
  public Abstraction<GroupKeys> groupKeys() {
    return groupKeys;
  }

  @Override
  public Abstraction<AggFuncs> aggFuncs() {
    return aggFuncs;
  }

  @Override
  public String toString() {
    return "Agg";
  }
}
