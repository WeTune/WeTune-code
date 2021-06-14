package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.Tracer;

import static sjtu.ipads.wtune.prover.NullTracer.NULL_TRACER;

class EmptyContext implements DecisionContext {
  public static final DecisionContext NULL_CONTEXT = new EmptyContext();

  @Override
  public Tracer openTracer(String name) {
    return NULL_TRACER;
  }

  @Override
  public void closeTracer() {}

  @Override
  public Tracer currentTracer() {
    return NULL_TRACER;
  }
}
