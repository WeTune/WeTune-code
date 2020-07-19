package sjtu.ipads.wtune.stmt.statement;

import sjtu.ipads.wtune.stmt.context.AppContext;

import java.util.HashSet;
import java.util.Objects;

public class AltStatement extends Statement {
  public static final String KEY_KIND = "kind";
  public static final String KEY_RAW_SQL = "rawSql";

  private final Statement main;
  private String kind;

  public AltStatement(Statement main) {
    this.main = main;
  }

  public Statement main() {
    return main;
  }

  public String kind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  @Override
  public String appName() {
    return main.appName();
  }

  @Override
  public int stmtId() {
    return main.stmtId();
  }

  @Override
  public AppContext appContext() {
    return main.appContext();
  }

  @Override
  public void setAppName(String appName) {
    throw new UnsupportedOperationException("cannot set app name of alt statement");
  }

  @Override
  public void setStmtId(int stmtId) {
    throw new UnsupportedOperationException("cannot set stmt id of alt statement");
  }

  @Override
  public Statement registerToApp() {
    throw new UnsupportedOperationException("cannot register alt statement to app");
  }

  @Override
  public Statement copy() {
    final AltStatement copy = new AltStatement(this.main);
    copy.kind = this.kind;
    copy.rawSql = this.rawSql;
    copy.parsed = this.parsed.copy();
    copy.resolvedBy = new HashSet<>(resolvedBy);
    copy.failToResolveBy = new HashSet<>(failToResolveBy);
    copy.reResolve();
    return copy;
  }

  @Override
  public void delete(String cause) {
    // TODO
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    AltStatement that = (AltStatement) o;
    return Objects.equals(main, that.main) && Objects.equals(kind, that.kind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(main, kind);
  }

  @Override
  public String toString() {
    return String.format("<%s, %d, alt>", appName(), stmtId());
  }
}
