package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;

public interface FragmentProbe {
  Fragment fragment();

  SymbolNaming naming();

  String stringify();
}
