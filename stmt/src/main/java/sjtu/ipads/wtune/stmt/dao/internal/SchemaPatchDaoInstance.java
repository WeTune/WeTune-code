package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;

import java.util.List;

public class SchemaPatchDaoInstance {
  private static SchemaPatchDao INSTANCE;

  public static void register(SchemaPatchDao instance) {
    INSTANCE = instance;
  }

  private static SchemaPatchDao instance0() {
    if (INSTANCE == null) {
      throw new StmtException("no global statement dao");
    }

    return INSTANCE;
  }

  public static SchemaPatchDao instance() {
    return INSTANCE;
  }

  public static List<SchemaPatch> findByApp(String appName) {
    return INSTANCE.findByApp(appName);
  }

  public static void save(SchemaPatch patch) {
    INSTANCE.save(patch);
  }
}
