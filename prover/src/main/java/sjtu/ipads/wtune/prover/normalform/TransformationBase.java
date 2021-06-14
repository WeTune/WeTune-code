package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.DecisionContext;

import static sjtu.ipads.wtune.prover.normalform.EmptyContext.NULL_CONTEXT;

abstract class TransformationBase implements Transformation {
  protected DecisionContext ctx = NULL_CONTEXT;

  @Override
  public void setContext(DecisionContext ctx) {
    if (ctx == null) this.ctx = NULL_CONTEXT;
    else this.ctx = ctx;
  }
}
