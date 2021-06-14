package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;

import static sjtu.ipads.wtune.prover.NullProof.NULL_PROOF;

abstract class TransformationBase implements Transformation {
  protected Proof ctx = NULL_PROOF;

  @Override
  public void setProof(Proof ctx) {
    if (ctx == null) this.ctx = NULL_PROOF;
    else this.ctx = ctx;
  }
}
