package sjtu.ipads.wtune.stmt.dao;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.CalciteDbStatementDao;

import java.util.List;

public interface CalciteStatementDao {
  Statement findOne(String appName, int stmtId);

  Pair<Statement, Statement> findPair(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  static CalciteStatementDao instance() {
    return CalciteDbStatementDao.instance();
  }
}
