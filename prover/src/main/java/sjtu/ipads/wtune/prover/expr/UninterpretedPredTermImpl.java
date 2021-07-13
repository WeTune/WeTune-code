package sjtu.ipads.wtune.prover.expr;

import static java.util.Objects.requireNonNull;

import sjtu.ipads.wtune.prover.utils.Util;

final class UninterpretedPredTermImpl extends UExprBase implements UninterpretedPredTerm {
  private final Name name;
  private Tuple[] args;

  public UninterpretedPredTermImpl(Name name, Tuple[] args) {
    this.name = requireNonNull(name);
    this.args = requireNonNull(args);
  }

  @Override
  public void subst(Tuple t, Tuple rep) {
    requireNonNull(t);
    requireNonNull(rep);

    args = Util.subst(args, t, rep);
  }

  @Override
  public boolean uses(Tuple v) {
    for (Tuple arg : args) if (arg.uses(v)) return true;
    return false;
  }

  @Override
  protected UExprBase copy0() {
    return new UninterpretedPredTermImpl(name, args);
  }

  @Override
  public Name name() {
    return name;
  }

  @Override
  public Tuple[] tuple() {
    return args;
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append('[');
    Util.interpolateToString(name.toString(), args, builder);
    builder.append(']');
    return builder;
  }
}
