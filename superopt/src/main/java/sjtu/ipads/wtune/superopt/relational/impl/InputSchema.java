package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class InputSchema extends BaseRelationSchema<Input> {
  protected InputSchema(Input op) {
    super(op);
  }

  public static InputSchema create(Input input) {
    return new InputSchema(input);
  }

  @Override
  public SymbolicColumns columns(Interpretation ignored) {
    return SymbolicColumns.fromSingle(operator.relation());
  }
}
