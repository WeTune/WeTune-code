package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.Substitution;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Objects;

public class SubstitutionImpl implements Substitution {
  private final Graph source;
  private final Graph target;
  private final Interpretation sourceInterpretation;
  private final Interpretation targetInterpretation;
  private final ConstraintSet constraints;

  private SubstitutionImpl(
      Graph source,
      Graph target,
      Interpretation sourceInterpretation,
      Interpretation targetInterpretation,
      ConstraintSet constraints) {
    this.source = source;
    this.target = target;
    this.sourceInterpretation = sourceInterpretation;
    this.targetInterpretation = targetInterpretation;
    this.constraints = constraints;
  }

  public static SubstitutionImpl create(
      Graph source,
      Graph target,
      Interpretation sourceInterpretation,
      Interpretation targetInterpretation,
      ConstraintSet constraints) {
    return new SubstitutionImpl(
        source, target, sourceInterpretation, targetInterpretation, constraints);
  }

  @Override
  public Graph source() {
    return source;
  }

  @Override
  public Graph target() {
    return target;
  }

  @Override
  public Interpretation sourceInterpretation() {
    return sourceInterpretation;
  }

  @Override
  public Interpretation targetInterpretation() {
    return targetInterpretation;
  }

  @Override
  public ConstraintSet constraints() {
    return constraints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SubstitutionImpl that = (SubstitutionImpl) o;
    return Objects.equals(source, that.source)
        && Objects.equals(target, that.target)
        && Objects.equals(sourceInterpretation, that.sourceInterpretation)
        && Objects.equals(targetInterpretation, that.targetInterpretation)
        && Objects.equals(constraints, that.constraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, target, sourceInterpretation, targetInterpretation, constraints);
  }

  @Override
  public String toString() {
    return "Substitution{"
        + "\n  source="
        + source
        + "\n  target="
        + target
        + "\n  sParam="
        + sourceInterpretation
        + "\n  tParam="
        + targetInterpretation
        + "\n  constraints="
        + constraints
        + "\n}";
  }
}
