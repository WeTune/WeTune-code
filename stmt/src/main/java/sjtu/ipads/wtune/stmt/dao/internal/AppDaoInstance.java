package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.AppDao;

public class AppDaoInstance {
  private static AppDao INSTANCE;

  public static void register(AppDao instance) {
    INSTANCE = instance;
  }

  private static AppDao instance0() {
    if (INSTANCE == null) {
      throw new StmtException("no global app dao");
    }

    return INSTANCE;
  }

  public static AppDao instance() {
    return INSTANCE;
  }

  public static AppContext inflateOne(AppContext app) {
    return instance0().inflateOne(app);
  }
}
