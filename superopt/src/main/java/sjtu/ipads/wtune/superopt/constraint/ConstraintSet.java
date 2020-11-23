package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.constraint.impl.ConstraintSetImpl;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class ConstraintSet {
  public abstract Set<Constraint> constraints();

  public boolean add(Constraint constraint) {
    if (checkNonConflict(constraint)) {
      final Set<Constraint> existing = constraints();
      final Set<Constraint> transitives = new HashSet<>();
      for (Constraint c : existing) {
        final Constraint transitive = c.transitive(constraint);
        if (transitive != null && !existing.contains(transitive)) transitives.add(transitive);
      }
      if (addAll(transitives)) add0(constraint);
      return true;
    } else return false;
  }

  public boolean addAll(Collection<Constraint> constraints) {
    if (constraints.stream().allMatch(this::checkNonConflict)) {
      addAll0(constraints);
      return true;
    }
    return false;
  }

  public boolean addAll(ConstraintSet set) {
    return addAll(set.constraints());
  }

  public boolean checkNonConflict(Constraint constraint) {
    for (Constraint c : constraints()) if (c.isConflict(constraint)) return false;
    return true;
  }

  public boolean checkInterpretation(Interpretation interpretation) {
    for (Abstraction<?> abstraction : interpretation.abstractions()) {
      final Object assignment = interpretation.interpret(abstraction);
      for (Constraint constraint : constraints())
        if (!constraint.check(interpretation, abstraction, assignment)) return false;
    }

    return true;
  }

  public boolean contains(Constraint constraint) {
    return constraints().contains(constraint);
  }

  protected abstract void add0(Constraint constraint);

  protected void addAll0(Collection<Constraint> constraints) {
    constraints.forEach(this::add0);
  }

  public static ConstraintSet empty() {
    return ConstraintSetImpl.create();
  }

  public static ConstraintSet from(Set<Constraint> constraints) {
    return ConstraintSetImpl.create(constraints);
  }

  public static ConstraintSet fromCopy(ConstraintSet set) {
    return ConstraintSetImpl.create(set.constraints());
  }

  @Override
  public String toString() {
    return constraints().toString();
  }
}
