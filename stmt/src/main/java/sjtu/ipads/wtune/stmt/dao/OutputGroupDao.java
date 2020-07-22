package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.similarity.output.OutputSimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public interface OutputGroupDao extends Dao {
  void save(OutputSimGroup group);

  OutputSimGroup findOne(int groupId);

  List<OutputSimGroup> findByStmt(Statement statement);

  void truncate();

  void beginBatch();

  void endBatch();

  static OutputGroupDao instance() {
    return DaoInstances.get(OutputGroupDao.class);
  }
}
