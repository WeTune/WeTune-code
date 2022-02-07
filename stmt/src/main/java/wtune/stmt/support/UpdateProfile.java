package wtune.stmt.support;

import wtune.stmt.CalciteStmtProfile;
import wtune.stmt.dao.CalciteStatementDao;

public interface UpdateProfile {
  static void cleanCalcite() {
    CalciteStatementDao.instance().cleanProfileData();
  }

  static void updateCalciteProfile(CalciteStmtProfile stmtProfile) {
    CalciteStatementDao.instance().updateProfile(stmtProfile);
  }
}
