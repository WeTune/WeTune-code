package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Objects;

public class NonConflictConstraint implements Constraint {
  @Override
  public boolean check(Interpretation context, Abstraction<?> abstraction, Object interpretation) {
    final Object existing = context.interpret(abstraction);
    return existing == null || Objects.equals(existing, interpretation);
  }

  public static final NonConflictConstraint INSTANCE = new NonConflictConstraint();
}
