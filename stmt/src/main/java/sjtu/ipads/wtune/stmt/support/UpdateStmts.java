package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.OptStatementDao;

public interface UpdateStmts {
  static void cleanOptStmts(OptimizerType type) {
    OptStatementDao.instance(type).cleanOptStmts();
  }

  static void updateOptStmts(Statement stmt, OptimizerType type) {
    OptStatementDao.instance(type).updateOptStmts(stmt);
  }
}
