package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

public class Contradiction implements Constraint {
  private static final Contradiction INSTANCE = new Contradiction();

  private Contradiction() {}

  public static Contradiction create() {
    return Contradiction.INSTANCE;
  }

  @Override
  public boolean check(Interpretation context, ConstraintSet constraints) {
    return false;
  }
}
