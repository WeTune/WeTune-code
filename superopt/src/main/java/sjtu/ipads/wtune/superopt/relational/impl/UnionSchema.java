package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Union;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

public class UnionSchema extends BaseRelationSchema<Union> {
  protected UnionSchema(Union union) {
    super(union);
  }

  public static UnionSchema create(Union union) {
    return new UnionSchema(union);
  }

  @Override
  public ColumnSet symbolicColumns(Interpretation interpretation) {
    //    return super.columns(interpretation);
    final ColumnSet left = operator.prev()[0].outSchema().symbolicColumns(interpretation);
    final ColumnSet right = operator.prev()[1].outSchema().symbolicColumns(interpretation);
    if (left == null || right == null) return null;

    return ColumnSet.mask(operator, left, right); // only left
  }
}
