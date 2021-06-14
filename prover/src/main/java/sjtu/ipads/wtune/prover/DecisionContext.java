package sjtu.ipads.wtune.prover;

public interface DecisionContext {
  void addProof(String name, Proof proof);

  String makeProofName();

  static DecisionContext make() {
    return new DecisionContextImpl();
  }
}
