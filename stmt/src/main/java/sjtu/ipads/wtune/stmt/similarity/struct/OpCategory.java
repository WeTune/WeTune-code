package sjtu.ipads.wtune.stmt.similarity.struct;

public enum OpCategory {
  EQUAL,
  NOT_EQUAL,
  NUMERIC_COMPARE,
  STRING_MATCH,
  CONTAINS_BY,
  IN_SUBQUERY,
  EXISTS,
  OTHER,
  // extended
  ORDER_BY,
}
