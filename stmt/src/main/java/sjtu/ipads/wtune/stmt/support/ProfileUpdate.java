package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.stmt.CalciteStmtProfile;
import sjtu.ipads.wtune.stmt.dao.CalciteStatementDao;

public interface ProfileUpdate {
  static void cleanCalcite() {
    CalciteStatementDao.instance().cleanProfileData();
  }

  static void updateCalciteProfile(CalciteStmtProfile stmtProfile) {
    CalciteStatementDao.instance().updateProfile(stmtProfile);
  }
}
