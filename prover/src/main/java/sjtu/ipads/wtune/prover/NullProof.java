package sjtu.ipads.wtune.prover;

final class NullProof implements Proof {
  private String name, conclusion, premise;

  NullProof() {}

  @Override
  public Proof setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String name() {
    return "";
  }

  @Override
  public Proof append(String operation) {
    return this;
  }

  @Override
  public Proof setConclusion(String conclusion) {
    this.conclusion = conclusion;
    return this;
  }

  @Override
  public Proof setPremise(String premise) {
    this.premise = premise;
    return this;
  }

  @Override
  public String stringify() {
    final StringBuilder builder = new StringBuilder();

    if (name == null) builder.append("example ");
    else builder.append("lemma ").append(name);

    if (premise != null) builder.append(' ').append(premise);

    builder.append(" : ").append(conclusion).append(" := sorry");

    return builder.toString();
  }
}
