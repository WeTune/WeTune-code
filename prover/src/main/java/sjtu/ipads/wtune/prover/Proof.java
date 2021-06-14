package sjtu.ipads.wtune.prover;

public interface Proof {
  String name();

  void append(String operation);

  void setConclusion(String conclusion);

  void setPremise(String premise);

  String stringify();

  static Proof make(String name) {
    return new SimpleProof(name);
  }
}
