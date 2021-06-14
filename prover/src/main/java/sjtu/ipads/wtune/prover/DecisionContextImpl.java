package sjtu.ipads.wtune.prover;

import java.util.HashMap;
import java.util.Map;

public class DecisionContextImpl implements DecisionContext {
  private final Map<String, Proof> proofs;
  private int nextProofId = 0;

  DecisionContextImpl() {
    this.proofs = new HashMap<>(4);
  }

  @Override
  public String makeProofName() {
    return "lemma_" + (nextProofId++);
  }

  @Override
  public void addProof(String name, Proof proof) {
    proofs.put(name, proof);
  }
}
