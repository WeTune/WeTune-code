package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.constraint.impl.ConstraintSetImpl;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;

public abstract class ConstraintSet implements Iterable<Constraint> {
  public abstract Set<Constraint> constraints();

  /**
   * If not conflict with existing constraints, add the constraint as well as its transitive closure
   * to this set. Otherwise do nothing.
   *
   * @return if conflict with existing constraint.
   */
  public boolean add(Constraint constraint) {
    if (constraint.isTautology()) return true;
    if (constraint.isContradiction()) return false;
    if (!checkNonConflict(constraint)) return false;

    final Set<Constraint> transitiveClosure = transitiveOf(constraint);
    // don't call addAll to avoid double computation
    if (checkNonConflict(transitiveClosure)) {
      add0(constraint);
      addAll0(transitiveClosure);
    }
    return true;
  }

  /**
   * Batch `add`.
   *
   * <p>NOTE: this is not an atomic operation: some constraints may have added to the set before an
   * conflict is detected. BE CAREFUL when used this method.
   */
  public boolean addAll(Iterable<Constraint> constraints) {
    for (Constraint constraint : constraints) if (!add(constraint)) return false;
    return true;
  }

  /***/
  public Set<Constraint> transitiveOf(Constraint c0) {
    final Set<Constraint> checked = new HashSet<>();
    Set<Constraint> toCheck = new HashSet<>();
    Set<Constraint> newToCheck = new HashSet<>();
    toCheck.add(c0);

    while (!toCheck.isEmpty()) {
      for (Constraint constraint : toCheck)
        constraints().stream()
            .map(constraint::buildTransitive)
            .filter(Objects::nonNull)
            .filter(not(checked::contains))
            .filter(not(toCheck::contains))
            .forEach(newToCheck::add);
      checked.addAll(toCheck);
      toCheck = newToCheck;
      newToCheck = new HashSet<>();
    }

    return checked;
  }

  /**
   * Check if single constraint is conflict with existing constraint.
   *
   * <p>NOTE: transitive constraints are not checked.
   */
  public boolean checkNonConflict(Constraint constraint) {
    return constraint.isTautology()
        || (!constraint.isContradiction()
            && constraints().stream().noneMatch(constraint::isConflict));
  }

  /** Batch `checkNonConflict` */
  public boolean checkNonConflict(Iterable<Constraint> constraint) {
    return StreamSupport.stream(constraint.spliterator(), false).allMatch(this::checkNonConflict);
  }

  public boolean checkInterpretation(Interpretation interpretation) {
    for (Constraint constraint : constraints()) {
      if (!constraint.check(interpretation, this)) return false;
    }
    return true;
    //    return constraints().stream().allMatch(it -> it.check(interpretation));
  }

  public boolean contains(Constraint constraint) {
    if (constraint.isTautology()) return true;
    return constraints().contains(constraint);
  }

  public int size() {
    return constraints().size();
  }

  @Override
  public Iterator<Constraint> iterator() {
    return constraints().iterator();
  }

  @Override
  public Spliterator<Constraint> spliterator() {
    return constraints().spliterator();
  }

  protected abstract void add0(Constraint constraint);

  protected void addAll0(Collection<Constraint> constraints) {
    constraints.forEach(this::add0);
  }

  public static ConstraintSet empty() {
    return ConstraintSetImpl.create();
  }

  private static ConstraintSet IMMUTABLE_EMPTY = ConstraintSetImpl.create(emptySet());

  public static ConstraintSet immutableEmpty() {
    return IMMUTABLE_EMPTY;
  }

  public static ConstraintSet from(Set<Constraint> constraints) {
    return ConstraintSetImpl.create(constraints);
  }

  public static ConstraintSet fromCopy(ConstraintSet set) {
    return ConstraintSetImpl.create(new HashSet<>(set.constraints()));
  }

  @Override
  public String toString() {
    return constraints().toString();
  }
}
