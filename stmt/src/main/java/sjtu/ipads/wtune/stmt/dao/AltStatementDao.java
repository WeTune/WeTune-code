package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.statement.AltStatement;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public interface AltStatementDao extends Dao {
  List<AltStatement> findByStmt(Statement stmt);

  static AltStatementDao instance() {
    return DaoInstances.get(AltStatementDao.class);
  }
}
