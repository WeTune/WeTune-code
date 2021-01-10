package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;

public interface AppDao extends Dao {
  App findOne(String name);

  static AppDao instance() {
    return DaoInstances.get(AppDao.class);
  }
}
