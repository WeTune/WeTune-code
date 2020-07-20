package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.TimingDaoInstance;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.stmt.statement.Timing;

import java.util.List;

public interface TimingDao {
  List<Timing> findByStmt(Statement stmt);

  void beginBatch();

  void endBatch();

  void insert(Timing timing);

  static TimingDao instance() {
    return TimingDaoInstance.instance();
  }

  default void registerAsGlobal() {
    TimingDaoInstance.register(this);
  }
}
