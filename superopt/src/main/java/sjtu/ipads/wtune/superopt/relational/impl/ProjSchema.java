package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class ProjSchema extends BaseRelationSchema<Proj> {
  protected ProjSchema(Proj proj) {
    super(proj);
  }

  public static ProjSchema create(Proj proj) {
    return new ProjSchema(proj);
  }

  @Override
  public boolean isStable() {
    return false;
  }

  @Override
  public SymbolicColumns columns(Interpretation interpretation) {
    final Projections projs = interpretation.interpret(operator.projs());
    return projs != null ? projs.columns() : null;
  }
}
