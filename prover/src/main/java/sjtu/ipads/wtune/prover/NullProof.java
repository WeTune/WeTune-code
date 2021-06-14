package sjtu.ipads.wtune.prover;

public final class NullProof implements Proof {
  public static final Proof NULL_PROOF = new NullProof();

  private String conclusion;

  private NullProof() {}

  @Override
  public String name() {
    return "";
  }

  @Override
  public void append(String operation) {}

  @Override
  public void setConclusion(String conclusion) {
    this.conclusion = conclusion;
  }

  @Override
  public void setPremise(String premise) {}

  @Override
  public String stringify() {
    return conclusion;
  }
}
