package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConstraintSetImpl extends ConstraintSet {
  private final Set<Constraint> constraints;

  private ConstraintSetImpl() {
    this(new HashSet<>());
  }

  private ConstraintSetImpl(Set<Constraint> constraints) {
    this.constraints = constraints;
  }

  public static ConstraintSetImpl create() {
    return new ConstraintSetImpl();
  }

  public static ConstraintSetImpl create(Set<Constraint> constraints) {
    return new ConstraintSetImpl(constraints);
  }

  @Override
  public void add0(Constraint constraint) {
    constraints.add(constraint);
  }

  @Override
  public Set<Constraint> constraints() {
    return constraints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConstraintSetImpl that = (ConstraintSetImpl) o;
    return Objects.equals(constraints, that.constraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(constraints);
  }
}
