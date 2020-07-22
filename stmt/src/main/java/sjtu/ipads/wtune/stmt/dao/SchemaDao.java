package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.schema.Schema;

public interface SchemaDao extends Dao {
  Schema findOne(String appName, String tag, String dbType);

  static SchemaDao instance() {
    return DaoInstances.get(SchemaDao.class);
  }
}
