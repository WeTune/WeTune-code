package sjtu.ipads.wtune.prover;

public interface Tracer {
  void trace(String operation);

  void setPrologue(String prologue);

  void setEpilogue(String epilogue);

  String getTrace();
}
