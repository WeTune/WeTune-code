package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.stmt.CalciteStmtProfile;
import sjtu.ipads.wtune.stmt.StmtProfile;
import sjtu.ipads.wtune.stmt.dao.CalciteOptStatementDao;

public interface ProfileUpdate {
  static void cleanCalcite() {
    CalciteOptStatementDao.instance().cleanProfileData();
  }

  static void updateCalciteProfile(CalciteStmtProfile stmtProfile) {
    CalciteOptStatementDao.instance().updateProfile(stmtProfile);
  }
}
