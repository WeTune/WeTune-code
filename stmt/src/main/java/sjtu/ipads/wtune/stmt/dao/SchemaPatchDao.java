package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.dao.internal.DbSchemaPatchDao;

import java.util.List;

public interface SchemaPatchDao {
  List<SchemaPatch> findByApp(String appName);

  void save(SchemaPatch patch);

  void truncate(String app);

  void beginBatch();

  void endBatch();

  static SchemaPatchDao instance() {
    return DbSchemaPatchDao.instance();
  }
}
