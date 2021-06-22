package sjtu.ipads.wtune.prover;

public interface Proof {
  String name();

  Proof setName(String name);

  Proof append(String operation);

  Proof setConclusion(String conclusion);

  Proof setPremise(String premise);

  String stringify();

  static Proof make() {
    return new SimpleProof();
  }

  static Proof makeNull() {
    return new NullProof();
  }
}
