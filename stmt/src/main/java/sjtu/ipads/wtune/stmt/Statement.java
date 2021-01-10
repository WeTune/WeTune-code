package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.internal.StatementImpl;

import java.util.List;

public interface Statement {
  String TAG_OPT = "opt";

  String appName();

  int stmtId();

  String rawSql();

  String stackTrace();

  SQLNode parsed();

  String tag();

  void setStmtId(int stmtId);

  void setTag(String tag);

  Statement alternative(String tag);

  Timing timing(String tag);

  void save();

  void delete(String reason);

  default boolean isMain() {
    return "main".equals(tag());
  }

  default App appContext() {
    return App.find(appName());
  }

  static Statement build(String appName, String rawSql, String stackTrace) {
    return StatementImpl.build(appName, rawSql, stackTrace);
  }

  static Statement build(String appName, int stmtId, String rawSql, String stackTrace) {
    return StatementImpl.build(appName, stmtId, rawSql, stackTrace);
  }

  static Statement build(String appName, int stmtId, String tag, String rawSql, String stackTrace) {
    return StatementImpl.build(appName, stmtId, tag, rawSql, stackTrace);
  }

  static Statement findOne(String appName, int stmtId) {
    return StatementDao.instance().findOne(appName, stmtId);
  }

  static List<Statement> findByApp(String appName) {
    return StatementDao.instance().findByApp(appName);
  }

  static List<Statement> findAll() {
    return StatementDao.instance().findAll();
  }
}
