package sjtu.ipads.wtune.sqlparser.plan;

public enum OperatorType {
  // Replace this by sealed interface after google-java-format plugin support the future.
  INPUT(0, "Input"),
  INNER_JOIN(2, "InnerJoin"),
  LEFT_JOIN(2, "LeftJoin"),
  SIMPLE_FILTER(1, "Filter"),
  IN_SUB_FILTER(2, "SubFilter"),
  EXISTS_FILTER(2, "Exists"),
  PROJ(1, "Proj"),
  AGG(1, "Agg"),
  SORT(1, "Sort"),
  LIMIT(1, "Limit"),
  UNION(2, "Union");

  private final int numPredecessors;
  private final String text;

  OperatorType(int numPredecessors, String text) {
    this.numPredecessors = numPredecessors;
    this.text = text;
  }

  public int numPredecessors() {
    return numPredecessors;
  }

  public String text() {
    return text;
  }

  public boolean isValidOutput() {
    return this != INNER_JOIN
        && this != LEFT_JOIN
        && this != SIMPLE_FILTER
        && this != IN_SUB_FILTER
        && this != EXISTS_FILTER;
  }

  public boolean isJoin() {
    return this == LEFT_JOIN || this == INNER_JOIN;
  }

  public boolean isFilter() {
    return this == SIMPLE_FILTER || this == IN_SUB_FILTER;
  }

  public static OperatorType parse(String value) {
    return switch (value) {
      case "LeftJoin" -> LEFT_JOIN;
      case "InnerJoin" -> INNER_JOIN;
      case "PlainFilter", "SimpleFilter", "Filter" -> SIMPLE_FILTER;
      case "SubqueryFilter", "InSubFilter" -> IN_SUB_FILTER;
      case "Input" -> INPUT;
      case "Proj", "Proj*" -> PROJ;
      default -> throw new IllegalArgumentException("unknown operator: " + value);
    };
  }
}
