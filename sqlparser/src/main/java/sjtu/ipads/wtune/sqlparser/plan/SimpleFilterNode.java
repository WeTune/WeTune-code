package sjtu.ipads.wtune.sqlparser.plan;

public interface SimpleFilterNode extends FilterNode {
  @Override
  default OperatorType kind() {
    return OperatorType.SIMPLE_FILTER;
  }

  static SimpleFilterNode mk(Expr predicate, RefBag refs) {
    return SimpleFilterNodeImpl.mk(predicate, refs);
  }
}
