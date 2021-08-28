package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.partialConstraintsOf;

class FragmentProbeImpl implements FragmentProbe {
  private final Fragment fragment;
  private final Constraints constraints;

  private SymbolNaming naming;
  private String stringifyCache;

  FragmentProbeImpl(Substitution substitution, boolean lhs) {
    this.fragment = lhs ? substitution._0() : substitution._1();
    this.constraints = partialConstraintsOf(substitution, lhs);
  }

  @Override
  public Fragment fragment() {
    return fragment;
  }

  @Override
  public SymbolNaming naming() {
    if (naming == null) {
      naming = SymbolNaming.mk();
      naming.name(fragment.symbols());
    }
    return naming;
  }

  @Override
  public String stringify() {
    if (stringifyCache == null) {
      final StringBuilder builder = new StringBuilder();
      builder.append(fragment.stringify(naming()));
      builder.append('|');
      constraints.canonicalStringify(naming(), builder);
      stringifyCache = builder.toString();
    }
    return stringifyCache;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FragmentProbe)) return false;
    final FragmentProbe that = (FragmentProbe) o;
    return stringify().equals(that.stringify());
  }

  @Override
  public int hashCode() {
    return stringify().hashCode();
  }
}
