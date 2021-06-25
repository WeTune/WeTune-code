package sjtu.ipads.wtune.prover;

public interface ProofContext {
  Proof newProof();

  Proof getProof(int idx);

  Proof getProof(String name);

  void pushScope();

  void popScope();
}
