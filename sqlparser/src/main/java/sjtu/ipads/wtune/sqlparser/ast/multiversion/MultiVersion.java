package sjtu.ipads.wtune.sqlparser.ast.multiversion;

public interface MultiVersion {
  void derive();

  Snapshot snapshot();

  void setSnapshot(Snapshot snapshot);
}
