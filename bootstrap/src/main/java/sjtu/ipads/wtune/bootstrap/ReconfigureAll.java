package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.reconfiguration.Reconfiguration;
import sjtu.ipads.wtune.reconfiguration.ReconfigurationOutput;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.scriptgen.ScriptUtils;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public class ReconfigureAll implements Task {
  @Override
  public void doTask(String appName) {

    final List<Statement> stmts = Statement.findByApp(appName);
    final ReconfigurationOutput output = Reconfiguration.reconfigure(appName, stmts);

    if (output.appName == null) return;

    final SchemaPatchDao dao = SchemaPatchDao.instance();
    dao.beginBatch();
    dao.truncate();
    output.patches().forEach(SchemaPatch::save);
    dao.endBatch();

    ScriptUtils.genSchemaPatch(output.patches(), appName);
    //    final Statement stmt = Statement.findOne("eladmin", 104);
    //    Reconfiguration.reconfigure(null, Collections.singletonList(stmt));
  }
}
