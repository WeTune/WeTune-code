package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

import java.util.List;

public interface Substitution {
  int id();

  Fragment _0();

  Fragment _1();

  Constraints constraints();

  SymbolNaming naming();

  String canonicalStringify();

  static Substitution parse(String str) {
    return SubstitutionImpl.parse(str);
  }

  static Substitution mk(Fragment f0, Fragment f1, List<Constraint> constraints) {
    return SubstitutionImpl.mk(f0, f1, constraints);
  }
}
