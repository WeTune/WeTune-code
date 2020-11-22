package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.impl.InterpretationImpl;

import java.util.Set;

public interface Interpretation {
  <T> T interpret(Abstraction<T> abs);

  boolean assign(Abstraction<?> abs, Object interpretation);

  Set<Abstraction<?>> abstractions();

  Set<Constraint> constraints();

  Interpretation merge(Interpretation other);

  void clearAssignment();

  void addConstraint(Constraint constraint);

  static Interpretation create() {
    return InterpretationImpl.create();
  }
}
