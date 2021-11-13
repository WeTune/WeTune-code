package sjtu.ipads.wtune.common.utils;

import java.util.ArrayList;
import java.util.List;

public interface ListSupport {
  static <T> List<T> fromIterable(Iterable<T> iterable) {
    final List<T> list = new ArrayList<>();
    for (T t : iterable) list.add(t);
    return list;
  }
}
