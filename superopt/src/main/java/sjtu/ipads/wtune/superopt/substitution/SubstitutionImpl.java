package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

class SubstitutionImpl implements Substitution {
  private final Fragment _0, _1;
  private final Constraints constraints;
  private final SymbolNaming naming;

  private int id;

  private SubstitutionImpl(
      Fragment fragment, Fragment fragment1, Constraints constraints, SymbolNaming naming) {
    _0 = fragment;
    _1 = fragment1;
    this.constraints = constraints;
    this.naming = naming;
  }

  static Substitution parse(String str) {
    final String[] split = str.split("\\|");
    if (split.length != 3)
      throw new IllegalArgumentException("invalid serialized substitution: " + str);

    final SymbolNaming naming = SymbolNaming.mk();
    final Fragment _0 = Fragment.parse(split[0], naming), _1 = Fragment.parse(split[1], naming);
    final List<Constraint> constraints =
        listMap(split[2].split(";"), it -> Constraint.parse(it, naming));

    return new SubstitutionImpl(_0, _1, Constraints.mk(constraints), naming);
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public Fragment _0() {
    return _0;
  }

  @Override
  public Fragment _1() {
    return _1;
  }

  @Override
  public Constraints constraints() {
    return constraints;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(50);
    _0.stringify(naming, builder).append('|');
    _1.stringify(naming, builder).append('|');
    constraints.stringify(naming, builder);
    return builder.toString();
  }
}
