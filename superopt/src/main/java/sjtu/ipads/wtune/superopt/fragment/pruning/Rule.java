package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.Fragment;

public interface Rule {
  boolean match(Fragment g);
}
