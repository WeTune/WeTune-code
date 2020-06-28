package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.internal.AppDaoInstance;

public interface AppDao {
  AppContext inflateOne(AppContext ctx);

  default void registerAsGlobal() {
    AppDaoInstance.register(this);
  }
}
