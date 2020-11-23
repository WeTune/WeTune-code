package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Union;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class UnionSchema extends BaseRelationSchema<Union> {
  protected UnionSchema(Union union) {
    super(union);
  }

  public static UnionSchema create(Union union) {
    return new UnionSchema(union);
  }

  @Override
  public boolean isStable() {
    return false;
  }

  @Override
  public SymbolicColumns columns(Interpretation interpretation) {
    //    return super.columns(interpretation);
    final SymbolicColumns left = operator.prev()[0].outSchema().columns(interpretation);
    final SymbolicColumns right = operator.prev()[1].outSchema().columns(interpretation);
    if (left == null || right == null) return null;

    return SymbolicColumns.mask(operator, left, right); // only left
  }
}
