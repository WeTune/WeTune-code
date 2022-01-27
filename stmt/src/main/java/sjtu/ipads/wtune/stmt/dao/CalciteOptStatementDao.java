package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.CalciteDbOptStatementDao;

import java.util.List;

public interface CalciteOptStatementDao {
  Statement findOne(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  static CalciteOptStatementDao instance() {
    return CalciteDbOptStatementDao.instance();
  }
}
