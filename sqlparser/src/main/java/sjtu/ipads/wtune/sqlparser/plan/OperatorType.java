package sjtu.ipads.wtune.sqlparser.plan;

public enum OperatorType {
  // Replace this by sealed interface after google-java-format plugin support the future.
  Input(0),
  InnerJoin(2),
  LeftJoin(2),
  PlainFilter(1),
  SubqueryFilter(2),
  Proj(1),
  Agg(1),
  Sort(1),
  Limit(1),
  Union(2),
  Distinct(1);

  private final int numPredecessors;

  OperatorType(int numPredecessors) {
    this.numPredecessors = numPredecessors;
  }

  public int numPredecessors() {
    return numPredecessors;
  }

  public boolean isValidOutput() {
    return this != InnerJoin && this != LeftJoin && this != PlainFilter && this != SubqueryFilter;
  }

  public boolean isJoin() {
    return this == LeftJoin || this == InnerJoin;
  }

  public boolean isFilter() {
    return this == PlainFilter || this == SubqueryFilter;
  }
}