package sjtu.ipads.wtune.common.multiversion;

public interface MultiVersion {
  Snapshot derive();

  void rollback(Snapshot snapshot);
}
