package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

public interface FragmentProbe {
  Fragment fragment();

  SymbolNaming naming();

  String stringify();
}
