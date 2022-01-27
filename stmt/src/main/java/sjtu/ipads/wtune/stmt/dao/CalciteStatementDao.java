package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.CalciteStmtProfile;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.CalciteDbStatementDao;

import java.util.List;

public interface CalciteStatementDao {
  Statement findOne(String appName, int stmtId);

  Statement findOneCalciteVersion(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  void cleanProfileData();

  void updateProfile(CalciteStmtProfile stmtProfile);

  static CalciteStatementDao instance() {
    return CalciteDbStatementDao.instance();
  }
}
