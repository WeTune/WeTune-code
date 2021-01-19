package sjtu.ipads.wtune.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public interface ISupplier<R> extends Supplier<R> {
  default List<R> repeat(int n) {
    final List<R> xs = new ArrayList<>(n);
    for (int i = 0; i < n; i++) xs.add(get());
    return xs;
  }
}
