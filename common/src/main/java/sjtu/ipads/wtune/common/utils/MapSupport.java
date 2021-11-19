package sjtu.ipads.wtune.common.utils;

import java.util.Map;

public interface MapSupport {
  static <K, V> Map<K, V> mkLazy() {
    return new LazyMap<>();
  }
}
