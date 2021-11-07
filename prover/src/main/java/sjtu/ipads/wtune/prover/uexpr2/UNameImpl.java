package sjtu.ipads.wtune.prover.uexpr2;

record UNameImpl(String str) implements UName {
  @Override
  public String toString() {
    return str;
  }
}
