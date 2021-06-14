package sjtu.ipads.wtune.prover;

import java.util.HashMap;
import java.util.Map;

import static sjtu.ipads.wtune.prover.NullTracer.NULL_TRACER;

public class DecisionContextImpl implements DecisionContext {
  private Tracer currentTracer;
  private final Map<String, Tracer> tracers;

  DecisionContextImpl() {
    this.tracers = new HashMap<>(4);
  }

  @Override
  public Tracer currentTracer() {
    return currentTracer == null ? NULL_TRACER : currentTracer;
  }

  @Override
  public Tracer openTracer(String name) {
    final Tracer tracer = new SimpleTracer();
    tracers.put(name, tracer);
    return currentTracer = tracer;
  }

  @Override
  public void closeTracer() {
    this.currentTracer = null;
  }
}
