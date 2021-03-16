package sjtu.ipads.wtune.superopt.optimizer.internal;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.superopt.util.Constraints;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickFrom;

import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.listSort;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.superopt.internal.Canonicalization.canonicalize;

public class SubstitutionImpl implements Substitution {
  private final Fragment g0, g1;
  private final Constraints constraints;
  private final Numbering numbering;

  private SubstitutionImpl(
      Fragment g0, Fragment g1, Numbering numbering, List<Constraint> constraints) {
    this.g0 = g0;
    this.g1 = g1;
    this.numbering = numbering;
    this.constraints = new Constraints(listSort(constraints, this::compare));
  }

  public static Substitution build(
      Fragment g0, Fragment g1, Numbering numbering, List<Constraint> constraints) {
    return new SubstitutionImpl(g0, g1, numbering, constraints);
  }

  public static Substitution build(String str) {
    final String[] split = str.split("\\|");
    if (split.length != 3)
      throw new IllegalArgumentException("invalid serialized substitution: " + str);

    final Fragment g0 = Fragment.rebuild(split[0]), g1 = Fragment.rebuild(split[1]);
    final Numbering numbering = Numbering.make().number(g0, g1);

    final List<Constraint> constraints =
        listMap(func2(SubstitutionImpl::rebuildConstraint).bind1(numbering), split[2].split(";"));

    canonicalize(g0);
    canonicalize(g1);

    final Numbering canonicalNumbering = Numbering.make().number(g0, g1);
    return new SubstitutionImpl(g0, g1, canonicalNumbering, constraints);
  }

  @Override
  public Fragment g0() {
    return g0;
  }

  @Override
  public Fragment g1() {
    return g1;
  }

  @Override
  public Numbering numbering() {
    return numbering;
  }

  @Override
  public Constraints constraints() {
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
            stream(constraints).map(this::toString).collect(Collectors.joining(";")));
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
    final Constraint.Kind kind = Constraint.Kind.valueOf(split[0]);
    final Placeholder[] targets = new Placeholder[split.length - 1];
    for (int i = 1; i < split.length; i++) targets[i - 1] = lookup.placeholderOf(split[i]);
    return Constraint.make(kind, targets);
  }
}
