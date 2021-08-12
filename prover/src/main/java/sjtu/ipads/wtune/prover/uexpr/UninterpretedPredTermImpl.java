package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

import sjtu.ipads.wtune.prover.utils.UExprUtils;

final class UninterpretedPredTermImpl extends UExprBase implements UninterpretedPredTerm {
  private final Name name;
  private Var[] args;

  public UninterpretedPredTermImpl(Name name, Var[] args) {
    this.name = requireNonNull(name);
    this.args = requireNonNull(args);
  }

  @Override
  public void subst(Var t, Var rep) {
    requireNonNull(t);
    requireNonNull(rep);

    args = UExprUtils.substArgs(args, t, rep);
  }

  @Override
  public boolean uses(Var v) {
    for (Var arg : args) if (arg.uses(v)) return true;
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
  public Var[] vars() {
    return args;
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append('[');
    UExprUtils.interpolateVars(name.toString(), args, builder);
    builder.append(']');
    return builder;
  }
}
