package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.optimization.internal.SubstitutionImpl;
import sjtu.ipads.wtune.superopt.util.Constraints;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.ArrayList;
import java.util.List;

public interface Substitution {
  Fragment g0();

  Fragment g1();

  Numbering numbering();

  Constraints constraints();

  default Substitution flip() {
    return build(g1(), g0(), Numbering.make().number(g1(), g0()), new ArrayList<>(constraints()));
  }

  default Substitution copy() {
    return rebuild(toString());
  }

  static Substitution build(
      Fragment g0, Fragment g1, Numbering numbering, List<Constraint> constraints) {
    return SubstitutionImpl.build(g0, g1, numbering, constraints);
  }

  static Substitution rebuild(String str) {
    return SubstitutionImpl.build(str);
  }
}
