package sjtu.ipads.wtune.superopt.fragment1;

class SymbolImpl implements Symbol {
  private final Kind kind;

  SymbolImpl(Kind kind) {
    this.kind = kind;
  }

  @Override
  public Kind kind() {
    return kind;
  }
}
