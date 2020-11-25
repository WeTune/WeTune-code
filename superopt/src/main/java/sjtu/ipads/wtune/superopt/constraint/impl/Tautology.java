package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

public class Tautology implements Constraint {
  private static final Tautology INSTANCE = new Tautology();

  private Tautology() {}

  public static Tautology create() {
    return INSTANCE;
  }

  @Override
  public boolean check(Interpretation context, ConstraintSet constraint) {
    return true;
  }
}
