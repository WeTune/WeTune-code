package sjtu.ipads.wtune.stmt.dao;

import java.util.List;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.DbOptStatementDao;

public interface OptStatementDao {
  Statement findOne(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  static OptStatementDao instance() {
    return DbOptStatementDao.instance();
  }
}
