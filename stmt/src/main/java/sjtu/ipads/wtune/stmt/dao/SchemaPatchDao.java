package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.SchemaPatchDaoInstance;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;

import java.util.List;

public interface SchemaPatchDao {
  List<SchemaPatch> findByApp(String appName);

  void save(SchemaPatch patch);

  default void registerAsGlobal() {
    SchemaPatchDaoInstance.register(this);
  }
}
