package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickFrom;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.listSort;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;

public class SubstitutionImpl implements Substitution {
  private final Plan g0, g1;
  private final List<Constraint> constraints;
  private final PlaceholderNumbering numbering;

  private SubstitutionImpl(
      Plan g0, Plan g1, PlaceholderNumbering numbering, List<Constraint> constraints) {
    this.g0 = g0;
    this.g1 = g1;
    this.numbering = numbering;
    this.constraints = listSort(constraints, this::compare);
  }

  public static Substitution build(
      Plan g0, Plan g1, PlaceholderNumbering numbering, List<Constraint> constraints) {
    return new SubstitutionImpl(g0, g1, numbering, constraints);
  }

  public static Substitution build(String str) {
    final String[] split = str.split("\\|");
    if (split.length != 3)
      throw new IllegalArgumentException("invalid serialized substitution: " + str);

    final Plan g0 = Plan.rebuild(split[0]), g1 = Plan.rebuild(split[1]);
    final PlaceholderNumbering numbering = PlaceholderNumbering.build();
    numbering.number(g0, g1);

    final List<Constraint> constraints =
        listMap(func2(SubstitutionImpl::rebuildConstraint).bind1(numbering), split[2].split(";"));

    return new SubstitutionImpl(g0, g1, numbering, constraints);
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
            g0.toString(numbering),
            g1.toString(numbering),
            constraints.stream().map(this::toString).collect(Collectors.joining(";")));
  }

  private static Constraint rebuildConstraint(String str, PlaceholderNumbering lookup) {
    final String[] split = str.split("[(),\\[\\]]+");
    switch (Constraint.Kind.valueOf(split[0])) {
      case TableEq:
        return Constraint.tableEq(lookup.find(split[1]), lookup.find(split[2]));
      case PickEq:
        return Constraint.pickEq(lookup.find(split[1]), lookup.find(split[2]));
      case PredicateEq:
        return Constraint.predicateEq(lookup.find(split[1]), lookup.find(split[2]));
      case PickFrom:
        return Constraint.pickFrom(
            lookup.find(split[1]),
            (Object[]) arrayMap(lookup::find, Indexed.class, Commons.subArray(split, 2)));
      case Reference:
        return Constraint.reference(
            lookup.find(split[1]),
            lookup.find(split[2]),
            lookup.find(split[3]),
            lookup.find(split[4]));
      default:
        throw new IllegalStateException();
    }
  }

  private String toString(Constraint constraint) {
    if (constraint instanceof PickFrom) {
      return constraint.kind()
          + "("
          + numbering.nameOf((Placeholder) ((PickFrom<?, ?>) constraint).p())
          + ",["
          + String.join(
              ",",
              listMap(it -> numbering.nameOf((Placeholder) it), ((PickFrom<?, ?>) constraint).ts()))
          + "])";
    } else
      return constraint.kind()
          + "("
          + String.join(
              ",", listMap(it -> numbering.nameOf((Placeholder) it), constraint.targets()))
          + ")";
  }

  private int compare(Constraint c0, Constraint c1) {
    final int res = c0.kind().compareTo(c1.kind());
    return res != 0 ? res : toString(c0).compareTo(toString(c1));
  }
}
