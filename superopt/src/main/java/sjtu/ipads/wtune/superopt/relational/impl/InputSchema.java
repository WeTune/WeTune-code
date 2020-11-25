package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

public class InputSchema extends BaseRelationSchema<Input> {
  private final ColumnSet columns;

  protected InputSchema(Input op) {
    super(op);
    this.columns = ColumnSet.nativeColumns(op, op.source());
  }

  public static InputSchema create(Input input) {
    return new InputSchema(input);
  }

  @Override
  public ColumnSet symbolicColumns(Interpretation ignored) {
    return columns;
  }
}
