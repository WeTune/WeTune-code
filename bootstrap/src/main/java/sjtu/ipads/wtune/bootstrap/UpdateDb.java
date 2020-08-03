package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.FingerprintDao;
import sjtu.ipads.wtune.stmt.dao.TimingDao;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Timing;

import static sjtu.ipads.wtune.stmt.statement.Statement.*;

public class UpdateDb implements Task {
  @Override
  public void doTask(String appName) {
    final AppContext app = AppContext.of(appName);

    TimingDao.instance().beginBatch();

    updateTiming(app, TAG_BASE);
//    updateTiming(app, TAG_OPT);
    updateTiming(app, TAG_PATCHED);
    updateTiming(app, TAG_BASE_B);
    updateTiming(app, TAG_BASE_C);
    updateTiming(app, TAG_BASE_D);
    updateTiming(app, TAG_OPT_B);
    updateTiming(app, TAG_OPT_C);
    updateTiming(app, TAG_OPT_D);

    TimingDao.instance().endBatch();

    FingerprintDao.instance().beginBatch();

    System.out.printf("[UpdateDb] updating fingerprint from %s/sample\n", appName);
    app.fingerprints().forEach(OutputFingerprint::save);

    FingerprintDao.instance().endBatch();
  }

  private static final void updateTiming(AppContext app, String tag) {
    System.out.printf("[UpdateDb] updating perf from %s/eval.%s\n", app.name(), tag);
    app.timing(tag).forEach(Timing::save);
  }
}
