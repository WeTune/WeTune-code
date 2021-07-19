package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;

public interface Substitution {
  int id();

  Fragment _0();

  Fragment _1();

  Constraints constraints();

  void setId(int id);

  static Substitution parse(String str) {
    return SubstitutionImpl.parse(str);
  }
}
