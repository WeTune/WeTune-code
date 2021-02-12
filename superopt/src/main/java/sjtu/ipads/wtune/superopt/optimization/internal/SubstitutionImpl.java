package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.plan.Numbering;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.Placeholders;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickFrom;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.listSort;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.superopt.internal.Canonicalize.canonicalize;

public class SubstitutionImpl implements Substitution {
  private final Plan g0, g1;
  private final List<Constraint> constraints;
  private final Numbering numbering;

  private SubstitutionImpl(Plan g0, Plan g1, Numbering numbering, List<Constraint> constraints) {
    this.g0 = g0;
    this.g1 = g1;
    this.numbering = numbering;
    this.constraints = listSort(constraints, this::compare);
  }

  public static Substitution buildIdentity(Plan g0) {
    final Plan g1 = g0.copy();
    final Placeholders ps0 = g0.placeholders(), ps1 = g1.placeholders();

    final List<Constraint> constraints = new ArrayList<>(ps0.count());
    constraints.addAll(zipMap(Constraint::tableEq, ps0.tables(), ps1.tables()));
    constraints.addAll(zipMap(Constraint::pickEq, ps0.picks(), ps1.picks()));
    constraints.addAll(zipMap(Constraint::predicateEq, ps0.predicates(), ps1.predicates()));

    return new SubstitutionImpl(g0, g1, Numbering.make().number(g0, g1), constraints);
  }

  public static Substitution build(
      Plan g0, Plan g1, Numbering numbering, List<Constraint> constraints) {
    return new SubstitutionImpl(g0, g1, numbering, constraints);
  }

  public static Substitution build(String str) {
    final String[] split = str.split("\\|");
    if (split.length != 3)
      throw new IllegalArgumentException("invalid serialized substitution: " + str);

    final Plan g0 = Plan.rebuild(split[0]), g1 = Plan.rebuild(split[1]);
    final Numbering numbering = Numbering.make().number(g0, g1);

    final List<Constraint> constraints =
        listMap(func2(SubstitutionImpl::rebuildConstraint).bind1(numbering), split[2].split(";"));

    canonicalize(g0);
    canonicalize(g1);

    final Numbering canonicalNumbering = Numbering.make().number(g0, g1);
    return new SubstitutionImpl(g0, g1, canonicalNumbering, constraints);
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
  public Numbering numbering() {
    return numbering;
  }

  @Override
  public List<Constraint> constraints() {
    return constraints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return this.toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public String toString() {
    return "%s|%s|%s"
        .formatted(
            g0.toString(numbering),
            g1.toString(numbering),
            constraints.stream().map(this::toString).collect(Collectors.joining(";")));
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

  private static Constraint rebuildConstraint(String str, Numbering lookup) {
    final String[] split = str.split("[(),\\[\\]]+");
    switch (Constraint.Kind.valueOf(split[0])) {
      case TableEq:
        return Constraint.tableEq(lookup.placeholderOf(split[1]), lookup.placeholderOf(split[2]));
      case PickEq:
        return Constraint.pickEq(lookup.placeholderOf(split[1]), lookup.placeholderOf(split[2]));
      case PredicateEq:
        return Constraint.predicateEq(
            lookup.placeholderOf(split[1]), lookup.placeholderOf(split[2]));
      case PickFrom:
        return Constraint.pickFrom(
            lookup.placeholderOf(split[1]),
            (Object[]) arrayMap(lookup::placeholderOf, Indexed.class, Commons.subArray(split, 2)));
      case Reference:
        return Constraint.reference(
            lookup.placeholderOf(split[1]),
            lookup.placeholderOf(split[2]),
            lookup.placeholderOf(split[3]),
            lookup.placeholderOf(split[4]));
      default:
        throw new IllegalStateException();
    }
  }
}
