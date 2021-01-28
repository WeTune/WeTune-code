package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.AltStatementDao;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatementImpl implements Statement {
  private final String appName;
  private final String rawSql;
  private final String stackTrace;

  private int stmtId;
  private SQLNode parsed;
  private String tag;

  private Map<String, Statement> alternatives;

  protected StatementImpl(
      String appName, int stmtId, String tag, String rawSql, String stackTrace) {
    this.appName = appName;
    this.stmtId = stmtId;
    this.rawSql = rawSql;
    this.stackTrace = stackTrace;
    this.tag = tag;
  }

  public static Statement build(String appName, String rawSql, String stackTrace) {
    return build(appName, -1, rawSql, stackTrace);
  }

  public static Statement build(String appName, int stmtId, String rawSql, String stackTrace) {
    return build(appName, stmtId, "main", rawSql, stackTrace);
  }

  public static Statement build(
      String appName, int stmtId, String tag, String rawSql, String stackTrace) {
    return new StatementImpl(appName, stmtId, tag, rawSql, stackTrace);
  }

  @Override
  public String appName() {
    return appName;
  }

  @Override
  public int stmtId() {
    return stmtId;
  }

  @Override
  public String rawSql() {
    return rawSql;
  }

  @Override
  public String stackTrace() {
    return stackTrace;
  }

  @Override
  public SQLNode parsed() {
    if (parsed == null) {
      parsed = SQLParser.ofDb(App.of(appName).dbType()).parse(rawSql());
    }

    return parsed;
  }

  @Override
  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  @Override
  public void setTag(String tag) {
    this.tag = tag;
  }

  @Override
  public String tag() {
    return tag;
  }

  @Override
  public Statement alternative(String tag) {
    if (alternatives == null)
      alternatives =
          AltStatementDao.instance().findByStmt(appName(), stmtId()).stream()
              .collect(Collectors.toMap(Statement::tag, Function.identity()));

    return alternatives.get(tag);
  }

  @Override
  public String toString() {
    return "%s-%d".formatted(appName(), stmtId());
  }
}
