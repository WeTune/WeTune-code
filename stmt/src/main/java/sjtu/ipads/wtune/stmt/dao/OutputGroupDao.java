package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;

public interface OutputGroupDao extends Dao, GroupDao {
  static OutputGroupDao instance() {
    return DaoInstances.get(OutputGroupDao.class);
  }
}
