package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.common.utils.Copyable;

public interface Fragment extends Copyable<Fragment> {
  int id();

  Op root();

  Symbols symbols();

  void setId(int i);

  void setRoot(Op root);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  void acceptVisitor(OpVisitor visitor);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Fragment mk(/* nullable */ Op head) {
    return new FragmentImpl(head);
  }

  static Fragment mk(/* nullable */ Op head, Symbols symbols) {
    return new FragmentImpl(head, symbols);
  }

  static Fragment parse(String str, SymbolNaming naming) {
    return FragmentImpl.parse(str, naming);
  }
}
