package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;

import java.util.List;

public interface SchemaPatchDao extends Dao {
  List<SchemaPatch> findByApp(String appName);

  void save(SchemaPatch patch);

  void beginBatch();

  void endBatch();

  static SchemaPatchDao instance() {
    return DaoInstances.get(SchemaPatchDao.class);
  }
}
