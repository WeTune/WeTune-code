package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.Timing;

import java.util.List;

public interface TimingDao extends Dao {
  List<Timing> findByStmt(String appName, int stmtId);

  void beginBatch();

  void endBatch();

  void save(Timing timing);

  static TimingDao instance() {
    return DaoInstances.get(TimingDao.class);
  }
}
