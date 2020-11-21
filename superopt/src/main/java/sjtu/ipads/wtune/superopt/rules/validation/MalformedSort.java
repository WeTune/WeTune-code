package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.rules.BaseVisitorMatchingRule;

public class MalformedSort extends BaseVisitorMatchingRule {
  @Override
  public boolean enterSort(Sort op) {
    final Operator in = op.prev()[0];
    if ((in != null && !in.canBeQueryOut()) || (in instanceof Sort)) {
      matched = true;
      return false;
    }
    return true;
  }
}
