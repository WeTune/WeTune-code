package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.optimization.internal.SubstitutionImpl;
import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.List;

public interface Substitution {
  Plan g0();

  Plan g1();

  List<Constraint> constraints();

  static Substitution build(
      Plan g0, Plan g1, PlaceholderNumbering numbering, List<Constraint> constraints) {
    return SubstitutionImpl.build(g0, g1, numbering, constraints);
  }

  static Substitution rebuild(String str) {
    return SubstitutionImpl.build(str);
  }
}
