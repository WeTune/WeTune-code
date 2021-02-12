package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.NumberingImpl;

public interface Numbering {
  Numbering number(Plan... plans);

  int numberOf(Placeholder placeholder);

  Placeholder placeholderOf(String name);

  default String nameOf(Placeholder placeholder) {
    return placeholder.tag() + numberOf(placeholder);
  }

  static Numbering make() {
    return NumberingImpl.build();
  }
}
