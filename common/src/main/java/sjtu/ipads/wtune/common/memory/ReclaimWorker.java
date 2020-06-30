package sjtu.ipads.wtune.common.memory;

public interface ReclaimWorker {
  void register(Reclaimer reclaimer);

  void start();

  static ReclaimWorker singleThreaded() {
    return new SingleThreadReclaimWorker();
  }
}
