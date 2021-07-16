package sjtu.ipads.wtune.prover.uexpr;

public interface Name {
  static Name mk(String name) {
    return new NameImpl(name);
  }
}
