package sjtu.ipads.wtune.superopt.impl.legacy;

import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.impl.ColumnSetImpl;

public interface ColumnSet {
  Operator operator();

  static ColumnSet create(Operator op) {
    return ColumnSetImpl.create(op);
  }
}
