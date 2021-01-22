package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.internal.Placeholder;

public interface Join extends Operator {
  Placeholder leftFields();

  Placeholder rightFields();
}
