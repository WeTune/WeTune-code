package sjtu.ipads.wtune.sqlparser.multiversion;

public interface MultiVersion {
  void derive();

  Snapshot snapshot();

  void setSnapshot(Snapshot snapshot);

  default int versionNumber() {
    return snapshot().versionNumber();
  }
}
