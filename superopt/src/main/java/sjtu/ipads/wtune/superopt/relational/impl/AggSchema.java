package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

public class AggSchema extends BaseRelationSchema<Agg> {
  protected AggSchema(Agg op) {
    super(op);
  }

  public static AggSchema create(Agg agg) {
    return new AggSchema(agg);
  }

  @Override
  public ColumnSet symbolicColumns(Interpretation interpretation) {
    final GroupKeys keys = interpretation.interpret(operator.groupKeys());
    return keys == null ? null : keys.columns();
  }
}
