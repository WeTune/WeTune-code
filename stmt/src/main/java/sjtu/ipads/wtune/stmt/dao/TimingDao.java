package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DbTimingDao;
import sjtu.ipads.wtune.stmt.support.Timing;

import java.util.List;

public interface TimingDao {
  List<Timing> findByStmt(String appName, int stmtId);

  void save(Timing timing);

  void beginBatch();

  void endBatch();

  static TimingDao instance() {
    return DbTimingDao.instance();
  }
}
