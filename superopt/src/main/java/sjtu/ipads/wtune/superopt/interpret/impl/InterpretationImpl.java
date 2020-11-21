package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.*;

import static sjtu.ipads.wtune.superopt.Helper.ensureSize;

public class InterpretationImpl implements Interpretation {
  private final Map<Abstraction<?>, Integer> absMap = new HashMap<>();
  private final List<Object> assignments = new ArrayList<>();
  private final Set<Constraint> constraints = new HashSet<>();

  public static InterpretationImpl create() {
    return new InterpretationImpl();
  }

  private InterpretationImpl() {
    constraints.add(Constraint.nonConflict());
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

    assignments.add(interpretation);

    return true;
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

    ensureSize(assignments, idx);
    assignments.set(idx, abs);
    absMap.put(abs, idx);

    return true;
  }

  private boolean assignEq(Abstraction<?> abs, Object interpretation, int idx) {
    for (Constraint constraint : constraints)
      if (constraint instanceof EqConstraint) {
        final EqConstraint eq = (EqConstraint) constraint;
        final Abstraction<?> otherSide = eq.otherSide(abs);
        // here also need to check condition
        // assume we have constraint "c0 = c1" and "c1 is unique"
        // then interpreting c0 as a non-unique column should be disallowed
        if (otherSide != null && !addInterpretation0(otherSide, interpretation, idx)) return false;
      }

    return true;
  }
}
