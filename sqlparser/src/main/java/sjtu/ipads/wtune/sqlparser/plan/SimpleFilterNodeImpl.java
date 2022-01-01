package sjtu.ipads.wtune.sqlparser.plan;

class SimpleFilterNodeImpl implements SimpleFilterNode {
  private final Expression predicate;

  SimpleFilterNodeImpl(Expression predicate) {
    this.predicate = predicate;
  }

  @Override
  public Expression predicate() {
    return predicate;
  }
}
