package sjtu.ipads.wtune.prover.expr;

class NameImpl implements Name {
  private final String name;

  NameImpl(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
