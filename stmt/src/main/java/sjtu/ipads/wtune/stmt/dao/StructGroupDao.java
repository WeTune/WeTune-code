package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;

public interface StructGroupDao extends Dao, GroupDao {
  static StructGroupDao instance() {
    return DaoInstances.get(StructGroupDao.class);
  }
}
