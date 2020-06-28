package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.SchemaDaoInstance;
import sjtu.ipads.wtune.stmt.schema.Schema;

public interface SchemaDao {
  Schema findOne(String appName, String dbType);

  default void registerAsGlobal() {
    SchemaDaoInstance.register(this);
  }
}
