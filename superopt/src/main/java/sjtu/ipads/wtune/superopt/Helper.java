package sjtu.ipads.wtune.superopt;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class Helper {
  public static <T> T newInstance(Class<T> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private static final int[][][] SET_PARTITION_BITS = {
    {{0}},
    {{0, 0}, {0, 1}},
    {{0, 0, 0}, {0, 0, 1}, {0, 1, 0}, {1, 0, 0}, {0, 1, 2}},
    {
      {0, 0, 0, 0}, {0, 0, 0, 1}, {0, 0, 1, 0}, {0, 1, 0, 0}, {1, 0, 0, 0}, {0, 0, 1, 1},
      {0, 1, 0, 1}, {0, 1, 1, 0}, {0, 0, 1, 2}, {0, 1, 0, 2}, {0, 1, 2, 0}, {1, 0, 0, 2},
      {1, 0, 2, 0}, {1, 2, 0, 0}, {0, 1, 2, 3}
    }
  };

  public static int[][] setPartition(int setSize) {
    assert setSize <= 4;
    return SET_PARTITION_BITS[setSize - 1];
  }

  public static void ensureSize(Collection<?> c, int size) {
    while (c.size() < size) c.add(null);
  }
}
