package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.dao.Dao;

import java.util.HashMap;
import java.util.Map;

public class DaoInstances {
  private static final Map<Class<? extends Dao>, Dao> INSTANCES = new HashMap<>(16);

  public static void register(Class<? extends Dao> cls, Dao instance) {
    INSTANCES.put(findDaoSubInterface(cls), instance);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Dao> T get(Class<T> cls) {
    return (T) INSTANCES.get(cls);
  }

  private static Class<? extends Dao> findDaoSubInterface(Class<?> cls) {
    for (Class<?> intf : cls.getInterfaces())
      if (intf == Dao.class) return (Class<? extends Dao>) cls;
      else {
        final Class<? extends Dao> res = findDaoSubInterface(intf);
        if (res != null) return res;
      }
    return null;
  }
}
