package sjtu.ipads.wtune.solver.sql;

public enum Operator {
  EQ("=", 1),
  AND("AND", 0),
  OR("OR", 0),
  IN_SUB("IN", 1);
  private static final int LOGIC = 0, CMP = 1;

  private final String literal;
  private final int type;

  Operator(String literal, int type) {
    this.literal = literal;
    this.type = type;
  }

  public String literal() {
    return literal;
  }

  public boolean isLogic() {
    return type == LOGIC;
  }

  public boolean isCmp() {
    return type == CMP;
  }

  @Override
  public String toString() {
    return literal;
  }
}
