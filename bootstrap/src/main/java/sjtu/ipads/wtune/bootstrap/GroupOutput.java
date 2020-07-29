package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.dao.OutputGroupDao;
import sjtu.ipads.wtune.stmt.similarity.SimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

public class GroupOutput implements Task {

  @Override
  public void doTasks(String... appNames) {
    final SimGroup.Builder builder = SimGroup.outputGroupBuilder();
    Statement.findAll().forEach(builder::add);

    final OutputGroupDao dao = OutputGroupDao.instance();
    dao.beginBatch();
    dao.truncate();
    builder.build().forEach(SimGroup::save);
    dao.endBatch();
  }
}
