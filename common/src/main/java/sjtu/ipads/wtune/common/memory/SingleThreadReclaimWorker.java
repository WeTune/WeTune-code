package sjtu.ipads.wtune.common.memory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

class SingleThreadReclaimWorker implements ReclaimWorker {
  private final List<Reclaimer> reclaimers = new CopyOnWriteArrayList<>();
  private volatile boolean started;

  @Override
  public void register(Reclaimer reclaimer) {
    reclaimers.add(reclaimer);
  }

  private boolean reclaim0() {
    boolean didWork = false;
    for (Reclaimer reclaimer : reclaimers) if (reclaimer.reclaim()) didWork = true;
    return didWork;
  }

  private void reclaim() {
    int sleepTime = 0;
    while (true) {
      if (reclaim0()) {
        sleepTime = 0;
        continue;
      }

      if (sleepTime == 0) sleepTime = 1;
      else if (sleepTime < 128) sleepTime = sleepTime << 1;

      try {
        TimeUnit.MICROSECONDS.sleep(sleepTime);
      } catch (InterruptedException interrupt) {
        return;
      }
    }
  }

  @Override
  public void start() {
    if (started) throw new IllegalStateException("attempt to SingleThreadReclaimWorker twice");

    started = true;
    final Thread thread = new Thread(this::reclaim);
    thread.setDaemon(true);
    thread.setPriority(3);
    thread.start();
  }
}
