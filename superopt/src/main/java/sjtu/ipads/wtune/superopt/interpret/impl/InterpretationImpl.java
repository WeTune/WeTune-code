package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.constraint.impl.EqRefConstraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.*;

public class InterpretationImpl implements Interpretation {
  private Map<Abstraction<?>, Object> assignments;

  private InterpretationImpl(Map<Abstraction<?>, Object> assignments) {
    this.assignments = assignments != null ? assignments : new HashMap<>();
  }

  public static InterpretationImpl create() {
    return new InterpretationImpl(null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T interpret(Abstraction<T> abs) {
    return (T) assignments.get(abs);
  }

  @Override
  public boolean assign(Abstraction<?> abs, Object assignment) {
    final Object existing = assignments.get(abs);
    if (existing != null) return Objects.equals(existing, assignment);

    assignments.put(abs, assignment);
    return true;
  }

  @Override
  public Interpretation assignNew(Abstraction<?> abs, Object assignment) {
    final InterpretationImpl newInterpretation = create();
    newInterpretation.assignments.putAll(this.assignments);
    return newInterpretation.assign(abs, assignment) ? newInterpretation : null;
  }

  @Override
  public Set<Abstraction<?>> abstractions() {
    return assignments.keySet();
  }

  @Override
  public Collection<Object> assignments() {
    return assignments.values();
  }

  @Override
  public boolean assignEq(ConstraintSet constraints, boolean alsoCheck) {
    final InterpretationImpl newInterpretation = new InterpretationImpl(new HashMap<>(assignments));

    for (Abstraction<?> abstraction : abstractions()) {
      final Object assignment = interpret(abstraction);

      for (Constraint constraint : constraints) {
        if (!(constraint instanceof EqRefConstraint)) continue;
        final EqRefConstraint eq = (EqRefConstraint) constraint;
        final Abstraction<?> otherSide = eq.otherSide(abstraction);
        if (otherSide != null && !newInterpretation.assign(otherSide, assignment)) return false;
      }
    }

    if (alsoCheck && !constraints.checkInterpretation(newInterpretation)) return false;
    this.assignments = newInterpretation.assignments;
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append('{');
    for (Abstraction<?> abstraction : assignments.keySet())
      builder.append(abstraction).append("=\"").append(interpret(abstraction)).append("\", ");
    if (builder.length() > 1) builder.delete(builder.length() - 2, builder.length());
    builder.append('}');
    return builder.toString();
  }
}
