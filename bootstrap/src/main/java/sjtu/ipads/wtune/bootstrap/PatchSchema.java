package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.analyzer.ImpliedForeignKeyAnalyzer;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.stmt.attrs.AppAttrs.IMPLIED_FOREIGN_KEYS;
import static sjtu.ipads.wtune.stmt.schema.SchemaPatch.impliedFK;

public class PatchSchema implements Task {
  @Override
  public void doTask(String appName) {
    for (Statement stmt : Statement.findByApp(appName))
      stmt.analyze(ImpliedForeignKeyAnalyzer.class);

    final Set<Column> implied = AppContext.of(appName).get(IMPLIED_FOREIGN_KEYS);
    if (implied != null) {
      System.out.println(appName);
      for (Column column : implied) System.out.println("  " + column);

      SchemaPatchDao.instance().beginBatch();
      listMap(it -> impliedFK(appName, it), implied).forEach(SchemaPatch::save);
      SchemaPatchDao.instance().endBatch();
    }
  }
}
