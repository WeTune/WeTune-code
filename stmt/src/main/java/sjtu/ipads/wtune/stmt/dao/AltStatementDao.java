package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.DbAltStatementDao;

import java.util.List;

public interface AltStatementDao {
  List<Statement> findByStmt(String appName, int stmtId);

  static AltStatementDao instance() {
    return DbAltStatementDao.instance();
  }
}
