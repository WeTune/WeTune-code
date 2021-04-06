package sjtu.ipads.wtune.common.multiversion;

import java.util.Map;

public interface Snapshot {
  Object get(Class<?> key);

  void put(Class<?> key, Object obj);

  void merge(Snapshot other);

  Map<Class<?>, Object> objs();

  static Snapshot make() {
    return new SnapshotImpl();
  }
}
