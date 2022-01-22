package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.stmt.dao.*;
import sjtu.ipads.wtune.stmt.internal.StatementImpl;

import java.util.List;

public interface Statement {
  String appName();

  int stmtId();

  String rawSql();

  String stackTrace();

  boolean isRewritten();

  SqlNode ast();

  void setStmtId(int stmtId);

  void setRewritten(boolean rewritten);

  Statement rewritten();

  Statement original();

  default App app() {
    return App.of(appName());
  }

  static Statement mk(String appName, String rawSql, String stackTrace) {
    return StatementImpl.build(appName, rawSql, stackTrace);
  }

  static Statement mk(String appName, int stmtId, String rawSql, String stackTrace) {
    return StatementImpl.build(appName, stmtId, rawSql, stackTrace);
  }

  static Statement findOne(String appName, int stmtId) {
    if (appName.equals("calcite_test"))
      return CalciteStatementDao.instance().findOne(appName, stmtId);
    return StatementDao.instance().findOne(appName, stmtId);
  }

  static List<Statement> findByApp(String appName) {
    if (appName.equals("calcite_test"))
      return CalciteStatementDao.instance().findByApp(appName);
    return StatementDao.instance().findByApp(appName);
  }

  static List<Statement> findAll() {
    return StatementDao.instance().findAll();
  }

  static Statement findOneRewritten(String appName, int stmtId) {
    if (appName.equals("calcite_test"))
      return CalciteOptStatementDao.instance().findOne(appName, stmtId);
    return OptStatementDao.instance().findOne(appName, stmtId);
  }

  static List<Statement> findRewrittenByApp(String appName) {
    if (appName.equals("calcite_test"))
      return CalciteOptStatementDao.instance().findByApp(appName);
    return OptStatementDao.instance().findByApp(appName);
  }

  static List<Statement> findAllRewritten() {
    return OptStatementDao.instance().findAll();
  }

  static List<Statement> findAllRewrittenByBagSem() {
    return OptBagStatementDao.instance().findAll();
  }


  static List<Statement> findAllOfCalcite() {
    return CalciteStatementDao.instance().findAll();
  }

  static List<Statement> findAllRewrittenOfCalcite() {
    return CalciteOptStatementDao.instance().findAll();
  }
}
