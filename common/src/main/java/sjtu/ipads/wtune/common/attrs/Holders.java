package sjtu.ipads.wtune.common.attrs;

import com.google.common.collect.MapMaker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class Holders {
  private static final ThreadLocal<Map<Object, Map<String, Object>>> HOLDERS =
      ThreadLocal.withInitial(() -> new MapMaker().weakKeys().makeMap());

  static Map<String, Object> get(Object holder) {
    return HOLDERS.get().computeIfAbsent(holder, ignored -> new HashMap<>());
  }

  static int size() {
    final var o = new Object();
    final var m = HOLDERS.get();
    m.put(o, Collections.emptyMap());
    m.remove(o);
    int i = 0;
    for (var e : m.entrySet()) ++i;
    return i;
  }
}
