package sjtu.ipads.wtune.stmt;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.dao.OptStatementDao;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.internal.StatementImpl;

public interface Statement {
  String appName();

  int stmtId();

  String rawSql();

  String stackTrace();

  boolean isRewritten();

  ASTNode parsed();

  void setStmtId(int stmtId);

  void setRewritten(boolean rewritten);

  Statement rewritten();

  Statement original();

  default App app() {
    return App.of(appName());
  }

  static Statement make(String appName, String rawSql, String stackTrace) {
    return StatementImpl.build(appName, rawSql, stackTrace);
  }

  static Statement make(String appName, int stmtId, String rawSql, String stackTrace) {
    return StatementImpl.build(appName, stmtId, rawSql, stackTrace);
  }

  static Statement findOne(String appName, int stmtId) {
    return StatementDao.instance().findOne(appName, stmtId);
  }

  static List<Statement> findByApp(String appName) {
    return StatementDao.instance().findByApp(appName);
  }

  static List<Statement> findRewrittenByApp(String appName) {
    return OptStatementDao.instance().findByApp(appName);
  }

  static List<Statement> findAll() {
    return StatementDao.instance().findAll();
  }

  static List<Statement> findAllRewritten() {
    return OptStatementDao.instance().findAll();
  }
}
