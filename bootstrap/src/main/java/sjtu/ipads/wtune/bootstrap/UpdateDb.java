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

    System.out.printf("[UpdateDb] updating perf from %s/eval.%s\n", appName, TAG_BASE);
    app.timing(TAG_BASE).forEach(Timing::save);

    System.out.printf("[UpdateDb] updating perf from %s/eval.%s\n", appName, TAG_INDEX);
    app.timing(TAG_INDEX).forEach(Timing::save);

    System.out.printf("[UpdateDb] updating perf from %s/eval.%s\n", appName, TAG_OPT);
    app.timing(TAG_OPT).forEach(Timing::save);

    TimingDao.instance().endBatch();

    FingerprintDao.instance().beginBatch();

    System.out.printf("[UpdateDb] updating fingerprint from %s/sample\n", appName);
    app.fingerprints().forEach(OutputFingerprint::save);

    FingerprintDao.instance().endBatch();
  }
}
