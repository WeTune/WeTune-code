package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;

public interface AppDao extends Dao {
  AppContext inflateOne(AppContext ctx);

  static AppDao instance() {
    return DaoInstances.get(AppDao.class);
  }
}
