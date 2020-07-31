package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.dao.StructGroupDao;
import sjtu.ipads.wtune.stmt.similarity.SimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public class GroupStruct implements Task {
  @Override
  public void doTasks(String... appNames) {
    final SimGroup.Builder builder = SimGroup.structGroupBuilder();
    for (Statement stmt : Statement.findAll()) {
      stmt.retrofitStandard();
      builder.add(stmt);
    }
    final List<SimGroup> groups = builder.build();

    final StructGroupDao dao = StructGroupDao.instance();
    dao.beginBatch();
    dao.truncate();
    groups.forEach(SimGroup::save);
    dao.endBatch();
  }
}
