package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.CalciteDbStatementDao;

import java.util.List;

public interface CalciteStatementDao {
  Statement findOne(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  static CalciteStatementDao instance() {
    return CalciteDbStatementDao.instance();
  }
}
