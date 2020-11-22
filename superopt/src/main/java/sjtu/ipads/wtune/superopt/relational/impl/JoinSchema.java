package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

public class JoinSchema extends BaseRelationSchema<Join> {
  protected JoinSchema(Join join) {
    super(join);
  }

  public static JoinSchema create(Join join) {
    return new JoinSchema(join);
  }

  @Override
  public ColumnSet columns(Interpretation interpretation) {
    return null;
  }
}
