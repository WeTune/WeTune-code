package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.superopt.optimization.internal.SubstitutionImpl;
import sjtu.ipads.wtune.superopt.plan.Numbering;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.List;

public interface Substitution {
  Plan g0();

  Plan g1();

  Numbering numbering();

  List<Constraint> constraints();

  default Substitution copy() {
    return rebuild(toString());
  }

  static Substitution build(Plan g0, Plan g1, Numbering numbering, List<Constraint> constraints) {
    return SubstitutionImpl.build(g0, g1, numbering, constraints);
  }

  static Substitution rebuild(String str) {
    return SubstitutionImpl.build(str);
  }
}
