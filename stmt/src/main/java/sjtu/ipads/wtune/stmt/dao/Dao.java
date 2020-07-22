package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;

public interface Dao {
  default void registerAsGlobal() {
    DaoInstances.register(this.getClass(), this);
  }
}
