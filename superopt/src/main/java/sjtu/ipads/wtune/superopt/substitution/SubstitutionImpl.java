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
  private SymbolNaming naming;

  private int id;

  private SubstitutionImpl(
      Fragment fragment, Fragment fragment1, Constraints constraints, SymbolNaming naming) {
    _0 = fragment;
    _1 = fragment1;
    this.constraints = constraints;
    this.naming = naming;
  }

  static Substitution parse(String str) {
    return parse(str, false);
  }

  static Substitution parse(String str, boolean backwardCompatible) {
    final String[] split = str.split("\\|");
    if (split.length != 3)
      throw new IllegalArgumentException("invalid serialized substitution: " + str);

    final SymbolNaming naming = SymbolNaming.mk();
    final Fragment _0 = Fragment.parse(split[0], naming, backwardCompatible);
    final Fragment _1 = Fragment.parse(split[1], naming, backwardCompatible);
    final List<Constraint> constraints =
        listMap(split[2].split(";"), it -> Constraint.parse(it, naming));

    return new SubstitutionImpl(_0, _1, Constraints.mk(constraints), naming);
  }

  static Substitution mk(Fragment f0, Fragment f1, List<Constraint> constraints) {
    final Constraints cs =
        constraints instanceof Constraints
            ? (Constraints) constraints
            : Constraints.mk(constraints);

    return new SubstitutionImpl(f0, f1, cs, null);
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
  public SymbolNaming naming() {
    if (naming == null) {
      naming = SymbolNaming.mk();
      naming.name(_0.symbols());
      naming.name(_1.symbols());
    }
    return naming;
  }

  @Override
  public void resetNaming() {
    naming = null;
  }

  @Override
  public String canonicalStringify() {
    final StringBuilder builder = new StringBuilder(50);
    final SymbolNaming naming = naming();
    _0.stringify(naming, builder).append('|');
    _1.stringify(naming, builder).append('|');
    constraints.canonicalStringify(naming, builder);
    return builder.toString();
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(50);
    final SymbolNaming naming = naming();
    _0.stringify(naming, builder).append('|');
    _1.stringify(naming, builder).append('|');
    constraints.stringify(naming, builder);
    return builder.toString();
  }
}
