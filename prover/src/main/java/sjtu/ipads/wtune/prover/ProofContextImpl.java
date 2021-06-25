package sjtu.ipads.wtune.prover;

import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;
import java.util.ArrayList;
import java.util.List;

class ProofContextImpl implements ProofContext {
  private final List<Proof> proofStack;
  private final TIntStack scopes;
  private int nextProofId = 0;

  ProofContextImpl() {
    this.proofStack = new ArrayList<>();
    this.scopes = new TIntArrayStack();
  }

  @Override
  public Proof newProof() {
    final Proof proof = Proof.makeNull().setName("lemma_" + (nextProofId++));
    proofStack.add(proof);
    return proof;
  }

  @Override
  public Proof getProof(int idx) {
    if (idx >= 0) return proofStack.get(idx);
    else return proofStack.get(proofStack.size() - idx);
  }

  @Override
  public Proof getProof(String name) {
    if (name == null) return null;

    for (Proof proof : proofStack)
      if (name.equals(proof.name())) {
        return proof;
      }

    return null;
  }

  @Override
  public void pushScope() {
    scopes.push(proofStack.size());
  }

  @Override
  public void popScope() {
    final int scope = scopes.pop();
    proofStack.subList(scope, proofStack.size()).clear();
  }
}
