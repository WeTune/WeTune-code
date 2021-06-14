package sjtu.ipads.wtune.prover;

public final class NullTracer implements Tracer {
  public static final Tracer NULL_TRACER = new NullTracer();

  private NullTracer() {}

  @Override
  public void trace(String operation) {}

  @Override
  public void setEpilogue(String epilogue) {}

  @Override
  public void setPrologue(String prologue) {}

  @Override
  public String getTrace() {
    return "";
  }
}
