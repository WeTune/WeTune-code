package sjtu.ipads.wtune.prover;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;

public class SimpleProof implements Proof {
  private final String name;
  private final List<String> steps;
  private String premise;
  private String conclusion;

  SimpleProof(String name) {
    this.name = requireNonNull(name);
    this.steps = new ArrayList<>();
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void append(String operation) {
    steps.add(operation);
  }

  @Override
  public void setConclusion(String conclusion) {
    this.conclusion = conclusion;
  }

  @Override
  public void setPremise(String premise) {
    this.premise = premise;
  }

  @Override
  public String stringify() {
    final StringBuilder builder = new StringBuilder();
    builder.append("lemma ").append(name);

    if (premise != null) builder.append(' ').append(premise);

    builder.append(" : ").append(conclusion).append(" := begin\n  ");

    joining(",\n  ", steps, builder);
    builder.append("\nend");
    return builder.toString();
  }
}
