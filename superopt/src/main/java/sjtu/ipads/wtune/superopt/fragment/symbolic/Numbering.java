package sjtu.ipads.wtune.superopt.fragment.symbolic;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.internal.NumberingImpl;

public interface Numbering {
  Numbering number(Fragment... fragments);

  int numberOf(Placeholder placeholder);

  Placeholder placeholderOf(String name);

  default String nameOf(Placeholder placeholder) {
    return placeholder.tag() + numberOf(placeholder);
  }

  static Numbering make() {
    return NumberingImpl.build();
  }
}
