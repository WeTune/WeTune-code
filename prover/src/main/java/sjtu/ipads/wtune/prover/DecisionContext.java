package sjtu.ipads.wtune.prover;

public interface DecisionContext {
  Tracer currentTracer();

  Tracer openTracer(String name);

  void closeTracer();

  default void trace(String operation) {
    currentTracer().trace(operation);
  }

  static DecisionContext make() {
    return new DecisionContextImpl();
  }
}
