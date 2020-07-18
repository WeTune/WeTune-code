package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.SchemaDao;
import sjtu.ipads.wtune.stmt.schema.Schema;

public class SchemaDaoInstance {
  private static SchemaDao INSTANCE;

  public static void register(SchemaDao instance) {
    INSTANCE = instance;
  }

  private static SchemaDao instance0() {
    if (INSTANCE == null) {
      throw new StmtException("no global statement dao");
    }

    return INSTANCE;
  }

  public static SchemaDao instance() {
    return INSTANCE;
  }

  public static Schema findOne(String appName, String tag, String dbType) {
    return INSTANCE.findOne(appName, tag, dbType);
  }
}
