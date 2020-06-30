package sjtu.ipads.wtune.common.attrs;

import sjtu.ipads.wtune.common.memory.AutoReclaimRegion;
import sjtu.ipads.wtune.common.memory.ReclaimWorker;

import java.util.HashMap;
import java.util.Map;

final class Holders {
  private static final ReclaimWorker WORKER = ReclaimWorker.singleThreaded();
  private static final AutoReclaimRegion REGION = new AutoReclaimRegion(WORKER);

  static {
    WORKER.start();
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> get(Object holder) {
    return (Map<String, Object>) REGION.associate(holder, HashMap::new);
  }
}
