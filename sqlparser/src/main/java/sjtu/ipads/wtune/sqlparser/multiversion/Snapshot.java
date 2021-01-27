package sjtu.ipads.wtune.sqlparser.multiversion;

import sjtu.ipads.wtune.sqlparser.multiversion.internal.SnapshotImpl;

import java.util.Collection;

public interface Snapshot {
  int versionNumber();

  void setVersionNumber(int versionNumber);

  <T> T get(Class<T> keyClazz);

  Collection<Object> keys();

  Snapshot merge(Snapshot snapshot);

  static Snapshot singleton(Object key) {
    return SnapshotImpl.build(key);
  }

  static Snapshot empty() {
    return SnapshotImpl.build();
  }
}
