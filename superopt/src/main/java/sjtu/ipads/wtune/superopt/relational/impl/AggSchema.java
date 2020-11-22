package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public class AggSchema extends BaseRelationSchema<Agg> {
  protected AggSchema(Agg op) {
    super(op);
  }

  public static AggSchema create(Agg agg) {
    return new AggSchema(agg);
  }

  @Override
  public ColumnSet columns(Interpretation interpretation) {
    return null; // TODO
  }

  @Override
  public boolean schemaEquals(RelationSchema other, Interpretation interpretation) {
    return super.schemaEquals(other, interpretation);
  }
}
