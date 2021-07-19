package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface SimpleFilterNode extends FilterNode {
  @Override
  default OperatorType type() {
    return OperatorType.SIMPLE_FILTER;
  }

  static SimpleFilterNode mk(Expr predicate, RefBag refs) {
    return SimpleFilterNodeImpl.mk(predicate, refs);
  }
}
