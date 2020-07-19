package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Timing;

import static sjtu.ipads.wtune.stmt.statement.Statement.*;

public class UpdateDb implements Task {
  @Override
  public void doTask(String appName) {
    final AppContext app = AppContext.of(appName);
    app.timing(TAG_BASE).forEach(Timing::save);
    app.timing(TAG_INDEX).forEach(Timing::save);
    app.timing(TAG_OPT).forEach(Timing::save);
    app.fingerprints().forEach(OutputFingerprint::save);
  }
}
