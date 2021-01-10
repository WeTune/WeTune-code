package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.Statement;

import java.util.List;

public interface AltStatementDao extends Dao {
  List<Statement> findByStmt(String appName, int stmtId);

  static AltStatementDao instance() {
    return DaoInstances.get(AltStatementDao.class);
  }
}
