package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.stmt.statement.Timing;

import java.util.List;

public interface TimingDao extends Dao {
  List<Timing> findByStmt(Statement stmt);

  void beginBatch();

  void endBatch();

  void save(Timing timing);

  static TimingDao instance() {
    return DaoInstances.get(TimingDao.class);
  }
}
