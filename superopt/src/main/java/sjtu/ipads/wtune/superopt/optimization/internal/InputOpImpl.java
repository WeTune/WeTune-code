package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.superopt.optimization.InputOp;

public class InputOpImpl extends OperatorBase implements InputOp {
  private final Relation table;

  private InputOpImpl(Relation table) {
    this.table = table;
  }

  public static InputOp build(Relation table) {
    return new InputOpImpl(table);
  }

  @Override
  public Relation table() {
    return table;
  }
}
