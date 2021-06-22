package sjtu.ipads.wtune.prover;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;

import java.util.ArrayList;
import java.util.List;

public class SimpleProof implements Proof {
  private String name;
  private final List<String> steps;
  private String premise;
  private String conclusion;

  SimpleProof() {
    this.steps = new ArrayList<>();
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Proof setName(String name) {
    this.name = requireNonNull(name);
    return this;
  }

  @Override
  public Proof append(String operation) {
    steps.add(operation);
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

    builder.append(" : ").append(conclusion).append(" := begin\n  ");

    joining(",\n  ", steps, builder);
    builder.append("\nend");
    return builder.toString();
  }
}
