package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.similarity.SimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public interface GroupDao extends Dao {
  void save(SimGroup group);

  SimGroup findOne(int groupId);

  List<SimGroup> findByStmt(Statement statement);

  void truncate();

  void beginBatch();

  void endBatch();
}
