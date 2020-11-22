package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

public class InputSchema extends BaseRelationSchema<Input> {
  protected InputSchema(Input op) {
    super(op);
  }

  public static InputSchema create(Input input) {
    return new InputSchema(input);
  }

  @Override
  public ColumnSet columns(Interpretation ignored) {
    return null;
  }
}
