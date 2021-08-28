package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;

import java.util.List;

public interface Substitution {
  int id();

  Fragment _0();

  Fragment _1();

  Constraints constraints();

  SymbolNaming naming();

  void resetNaming();

  String canonicalStringify();

  FragmentProbe probe(boolean lhs);

  static Substitution parse(String str) {
    return parse(str, false);
  }

  static Substitution parse(String str, boolean backwardCompatible) {
    return SubstitutionImpl.parse(str, backwardCompatible);
  }

  static Substitution mk(Fragment f0, Fragment f1, List<Constraint> constraints) {
    return SubstitutionImpl.mk(f0, f1, constraints);
  }
}
