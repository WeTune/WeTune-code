package sjtu.ipads.wtune.superopt.substitution.internal;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.util.LockGuard;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.superopt.plan.Placeholder.numbering;
import static sjtu.ipads.wtune.superopt.util.Lockable.compose;

public class SubstitutionImpl implements Substitution {
  private final Plan g0, g1;
  private final List<Constraint> constraints;

  private SubstitutionImpl(Plan g0, Plan g1, List<Constraint> constraints) {
    this.g0 = g0;
    this.g1 = g1;
    constraints.sort(Constraint::compareTo);
    this.constraints = constraints;
  }

  public static Substitution build(Plan g0, Plan g1, List<Constraint> constraints) {
    try (LockGuard ignored = compose(g0, g1).guard()) {
      // Since multiple threads may number the graphs simultaneously but differently,
      // so we have to copy the placeholders with lock protected
      numbering(false).number(g0, g1);
      return new SubstitutionImpl(g0, g1, listMap(it -> it.unwrap(Placeholder::copy), constraints));
    }
  }

  public static Substitution build(String str) {
    final String[] split = str.split("\\|");
    if (split.length != 3)
      throw new IllegalArgumentException("invalid serialized substitution: " + str);

    final Plan g0 = Plan.rebuild(split[0]), g1 = Plan.rebuild(split[1]);
    final Map<String, Placeholder> placeholders = numbering().number(g0, g1).placeholders();

    final List<Constraint> constraints =
        listMap(it -> rebuildConstraint(it, placeholders), split[2].split(";"));

    return new SubstitutionImpl(g0, g1, constraints);
  }

  @Override
  public Plan g0() {
    return g0;
  }

  @Override
  public Plan g1() {
    return g1;
  }

  @Override
  public List<Constraint> constraints() {
    return constraints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SubstitutionImpl that = (SubstitutionImpl) o;
    return Objects.equals(g0, that.g0)
        && Objects.equals(g1, that.g1)
        && Objects.equals(constraints, that.constraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(g0, g1, constraints);
  }

  @Override
  public String toString() {
    return "%s|%s|%s"
        .formatted(
            g0.toInformativeString(),
            g1.toInformativeString(),
            constraints.stream().map(Object::toString).collect(Collectors.joining(";")));
  }

  private static Constraint rebuildConstraint(String str, Map<String, Placeholder> lookup) {
    final String[] split = str.split("[(),\\[\\]]+");
    switch (Constraint.Kind.valueOf(split[0])) {
      case TableEq:
        return Constraint.tableEq(lookup.get(split[1]), lookup.get(split[2]));
      case PickEq:
        return Constraint.pickEq(lookup.get(split[1]), lookup.get(split[2]));
      case PredicateEq:
        return Constraint.predicateEq(lookup.get(split[1]), lookup.get(split[2]));
      case PickFrom:
        return Constraint.pickFrom(
            lookup.get(split[1]), arrayMap(lookup::get, Indexed.class, Commons.subArray(split, 2)));
      case Reference:
        return Constraint.reference(
            lookup.get(split[1]), lookup.get(split[2]), lookup.get(split[3]), lookup.get(split[4]));
      default:
        throw new IllegalStateException();
    }
  }
}
