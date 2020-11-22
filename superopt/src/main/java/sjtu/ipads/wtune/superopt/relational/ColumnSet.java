package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.ColumnSetImpl;
import sjtu.ipads.wtune.superopt.operators.Operator;

public interface ColumnSet {
  Operator operator();

  static ColumnSet create(Operator op) {
    return ColumnSetImpl.create(op);
  }
}
