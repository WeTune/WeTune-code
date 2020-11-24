package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class InputSchema extends BaseRelationSchema<Input> {
  private final SymbolicColumns columns;

  protected InputSchema(Input op) {
    super(op);
    this.columns = SymbolicColumns.fromSingle(op, op.source());
  }

  @Override
  public boolean isStable() {
    return true;
  }

  public static InputSchema create(Input input) {
    return new InputSchema(input);
  }

  @Override
  public SymbolicColumns columns(Interpretation ignored) {
    return columns;
  }
}
