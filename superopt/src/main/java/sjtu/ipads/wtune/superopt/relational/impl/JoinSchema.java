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
  public ColumnSet symbolicColumns(Interpretation interpretation) {
    final ColumnSet left = operator.prev()[0].outSchema().symbolicColumns(interpretation);
    final ColumnSet right = operator.prev()[1].outSchema().symbolicColumns(interpretation);
    if (left == null || right == null) return null;

    return ColumnSet.union(left, right);
  }
}
