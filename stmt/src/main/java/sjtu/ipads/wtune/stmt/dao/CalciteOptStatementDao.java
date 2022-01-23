package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.StmtProfile;
import sjtu.ipads.wtune.stmt.dao.internal.CalciteDbOptStatementDao;

import java.util.List;

public interface CalciteOptStatementDao {
  Statement findOne(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  void cleanProfileData();

  void updateProfile(StmtProfile stmtProfile);

  static CalciteOptStatementDao instance() {
    return CalciteDbOptStatementDao.instance();
  }
}
