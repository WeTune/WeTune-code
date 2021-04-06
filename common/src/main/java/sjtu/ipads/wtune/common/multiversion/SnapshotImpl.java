package sjtu.ipads.wtune.common.multiversion;

import java.util.HashMap;
import java.util.Map;

class SnapshotImpl implements Snapshot {
  private Class<?> singleKey;
  private Object singleObj;

  private Map<Class<?>, Object> objs;

  @Override
  public void put(Class<?> key, Object obj) {
    if (obj != null && !key.isInstance(obj)) throw new IllegalArgumentException();

    if (objs != null) {
      objs.put(key, obj);
      return;
    }

    if (singleKey == null) {
      singleKey = key;
      singleObj = obj;
      return;
    }

    if (key.equals(singleKey)) {
      singleObj = obj;
      return;
    }

    objs = new HashMap<>();
    objs.put(singleKey, singleObj);
    objs.put(key, obj);

    singleKey = null;
    singleObj = null;
  }

  @Override
  public Object get(Class<?> key) {
    if (key.equals(singleKey)) return singleObj;
    else if (objs != null) return objs.get(key);
    else return null;
  }

  @Override
  public void merge(Snapshot other) {
    objs().forEach(this::put);
  }

  @Override
  public Map<Class<?>, Object> objs() {
    return objs;
  }
}
