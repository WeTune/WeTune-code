package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.impl.SubstitutionImpl;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

public interface Substitution {
  Graph source();

  Graph target();

  Interpretation sourceInterpretation();

  Interpretation targetInterpretation();

  ConstraintSet constraints();

  static Substitution create(
      Graph source,
      Graph target,
      Interpretation sourceInterpretation,
      Interpretation targetInterpretation,
      ConstraintSet constraints) {
    return SubstitutionImpl.create(
        source, target, sourceInterpretation, targetInterpretation, constraints);
  }
}
