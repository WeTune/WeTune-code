package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.Substitution;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

public class SubstitutionImpl implements Substitution {
  private final Graph source;
  private final Graph target;
  private final Interpretation interpretation;

  private SubstitutionImpl(Graph source, Graph target, Interpretation interpretation) {
    this.source = source;
    this.target = target;
    this.interpretation = interpretation;
  }

  @Override
  public Graph source() {
    return source;
  }

  @Override
  public Graph target() {
    return target;
  }

  @Override
  public Interpretation interpretation() {
    return interpretation;
  }
}
