package sjtu.ipads.wtune.stmt.resolver;

public class BoolExpr {
  private boolean isPrimitive;
  private boolean isJoinKey;

  public boolean isPrimitive() {
    return isPrimitive;
  }

  public boolean isJoinKey() {
    return isJoinKey;
  }

  public void setPrimitive(boolean primitive) {
    isPrimitive = primitive;
  }

  public void setJoinKey(boolean joinKey) {
    isJoinKey = joinKey;
  }
}
