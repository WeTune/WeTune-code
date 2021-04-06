package sjtu.ipads.wtune.sqlparser.ast.constants;

public enum ConstraintType {
  UNIQUE,
  PRIMARY,
  NOT_NULL,
  FOREIGN,
  CHECK;

  public static boolean isUnique(ConstraintType type) {
    return type == UNIQUE || type == PRIMARY;
  }
}
