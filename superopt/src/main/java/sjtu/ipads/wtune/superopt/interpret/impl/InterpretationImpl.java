package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.impl.EqRefConstraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.*;

import static sjtu.ipads.wtune.superopt.Helper.ensureSize;

public class InterpretationImpl implements Interpretation {
  private final Map<Abstraction<?>, Integer> absMap;
  private final List<Object> assignments;
  private final Set<Constraint> constraints;

  private InterpretationImpl(
      Map<Abstraction<?>, Integer> absMap, List<Object> assignments, Set<Constraint> constraints) {
    this.absMap = absMap != null ? absMap : new HashMap<>();
    this.assignments = assignments != null ? assignments : new ArrayList<>();
    this.constraints = constraints != null ? constraints : new HashSet<>();
    this.constraints.add(Constraint.nonConflict());
  }

  public static InterpretationImpl create() {
    return new InterpretationImpl(null, null, null);
  }

  public static InterpretationImpl create(Set<Constraint> constraints) {
    return new InterpretationImpl(null, null, constraints);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T interpret(Abstraction<T> abs) {
    final Integer key = absMap.get(abs);
    if (key == null || key >= assignments.size()) return null;
    return (T) assignments.get(key);
  }

  @Override
  public void addConstraint(Constraint constraint) {
    constraints.add(constraint);
  }

  @Override
  public boolean assign(Abstraction<?> abs, Object interpretation) {
    Integer idx = absMap.get(abs);
    if (idx == null) idx = assignments.size();

    if (!addInterpretation0(abs, interpretation, idx)) return false;
    if (!assignEq(abs, interpretation, idx)) return false;

    for (Constraint constraint : constraints) if (!constraint.recheck(this)) return false;

    return true;
  }

  @Override
  public Interpretation assignNew(Abstraction<?> abs, Object assignment) {
    final InterpretationImpl newInterpretation = create();
    newInterpretation.absMap.putAll(this.absMap);
    newInterpretation.assignments.addAll(this.assignments);
    newInterpretation.constraints.addAll(this.constraints);
    return newInterpretation.assign(abs, assignment) ? newInterpretation : null;
  }

  @Override
  public Interpretation merge(Interpretation other) {
    final Interpretation newInterpret = Interpretation.create();

    this.constraints().forEach(newInterpret::addConstraint);
    other.constraints().forEach(newInterpret::addConstraint);

    for (Abstraction<?> abstraction : this.abstractions())
      if (!newInterpret.assign(abstraction, interpret(abstraction))) return null;

    for (Abstraction<?> abstraction : other.abstractions())
      if (!newInterpret.assign(abstraction, other.interpret(abstraction))) return null;

    return newInterpret;
  }

  @Override
  public Set<Abstraction<?>> abstractions() {
    return absMap.keySet();
  }

  @Override
  public Set<Constraint> constraints() {
    return constraints;
  }

  @Override
  public void clearAssignment() {
    final ListIterator<Object> iter = assignments.listIterator();
    while (iter.hasNext()) {
      iter.next();
      iter.set(null);
    }
  }

  private boolean addInterpretation0(Abstraction<?> abs, Object interpretation, int idx) {
    for (Constraint constraint : constraints)
      if (!constraint.check(this, abs, interpretation)) return false;

    ensureSize(assignments, idx + 1);
    assignments.set(idx, interpretation);
    absMap.put(abs, idx);

    return true;
  }

  private boolean assignEq(Abstraction<?> abs, Object interpretation, int idx) {
    for (Constraint constraint : constraints)
      if (constraint instanceof EqRefConstraint) {
        final EqRefConstraint eq = (EqRefConstraint) constraint;
        final Abstraction<?> otherSide = eq.otherSide(abs);
        // here also need to check condition
        // assume we have constraint "c0 = c1" and "c1 is unique"
        // then interpreting c0 as a non-unique column should be disallowed
        if (otherSide != null && !addInterpretation0(otherSide, interpretation, idx)) return false;
      }

    return true;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append('{');
    for (Abstraction<?> abstraction : absMap.keySet())
      builder.append(abstraction).append(": ").append(interpret(abstraction)).append(", ");
    if (builder.length() > 1) builder.delete(builder.length() - 2, builder.length());
    builder.append('}');
    return builder.toString();
  }
}
