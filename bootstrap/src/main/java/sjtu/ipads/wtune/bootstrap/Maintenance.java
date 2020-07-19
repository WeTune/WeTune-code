package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.resolver.ParamResolver;
import sjtu.ipads.wtune.stmt.scriptgen.ParameterizedSQLFormatter;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Maintenance implements Task {
  private static void deduplicate(String appName) {
    final List<Statement> stmts = Statement.findByApp(appName);
    final Map<String, Integer> known = new HashMap<>();

    for (Statement stmt : stmts) {
      stmt.retrofitStandard();
      stmt.resolve(ParamResolver.class);
      final ParameterizedSQLFormatter formatter = new ParameterizedSQLFormatter();
      stmt.parsed().accept(formatter);

      final String str = formatter.toString();
      final Integer existing = known.get(str);
      if (existing != null) {
        System.out.printf(
            "[Maintenance] duplicate in %s: %d -> %d\n", appName, stmt.stmtId(), existing);
        stmt.delete("duplicated");

      } else known.put(str, stmt.stmtId());
    }
  }

  @Override
  public void doTask(String appName) {
    deduplicate(appName);
  }
}
