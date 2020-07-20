package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.TimingDao;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.stmt.statement.Timing;

import java.util.List;

public class TimingDaoInstance {
  private static TimingDao INSTANCE;

  public static void register(TimingDao instance) {
    INSTANCE = instance;
  }

  private static TimingDao instance0() {
    if (INSTANCE == null) {
      throw new StmtException("no global timing dao");
    }

    return INSTANCE;
  }

  public static TimingDao instance() {
    return INSTANCE;
  }

  public static List<Timing> findByStmt(Statement stmt) {
    return instance0().findByStmt(stmt);
  }

  public static void save(Timing timing) {
    instance0().insert(timing);
  }

  public static void beginBatch() {
    instance0().beginBatch();
  }

  public static void endBatch() {
    instance0().endBatch();
  }
}
