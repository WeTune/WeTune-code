package sjtu.ipads.wtune.stmt;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.stmt.dao.*;
import sjtu.ipads.wtune.stmt.internal.StatementImpl;
import sjtu.ipads.wtune.stmt.support.OptimizerType;

import java.util.List;

import static sjtu.ipads.wtune.stmt.support.OptimizerType.WeTune;

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

  // Find original statement(s)
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

  // Find optimized statement(s)
  static Statement findOneRewritten(String appName, int stmtId) {
    return findOneRewritten(appName, stmtId, WeTune);
  }
  static Statement findOneRewritten(String appName, int stmtId, OptimizerType type) {
    if (appName.equals("calcite_test"))
      return CalciteOptStatementDao.instance().findOne(appName, stmtId);
    return OptStatementDao.instance(type).findOne(appName, stmtId);
  }

  static List<Statement> findRewrittenByApp(String appName) {
    return findRewrittenByApp(appName, WeTune);
  }
  static List<Statement> findRewrittenByApp(String appName, OptimizerType type) {
    if (appName.equals("calcite_test"))
      return CalciteOptStatementDao.instance().findByApp(appName);
    return OptStatementDao.instance(type).findByApp(appName);
  }

  static List<Statement> findAllRewritten() {
    return findAllRewritten(WeTune);
  }
  static List<Statement> findAllRewritten(OptimizerType type) {
    return OptStatementDao.instance(type).findAll();
  }

  // Find-alls in calcite, and other interface of calcite
  static List<Statement> findAllOfCalcite() {
    return CalciteStatementDao.instance().findAll();
  }

  static List<Statement> findAllRewrittenOfCalcite() {
    return CalciteOptStatementDao.instance().findAll();
  }

  static Pair<Statement, Statement> findOriginalPairOfCalcite(String appName, int stmtId) {
    return CalciteStatementDao.instance().findPair(appName, stmtId);
  }
}
