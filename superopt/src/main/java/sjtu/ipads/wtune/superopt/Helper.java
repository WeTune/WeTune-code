package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.nCopies;

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

  public static int[][] partitionBits(int setSize) {
    assert setSize <= 4;
    return SET_PARTITION_BITS[setSize - 1];
  }

  public static void ensureSize(Collection<?> c, int size) {
    while (c.size() < size) c.add(null);
  }

  public static <T1, T2, R> Function<Pair<T1, T2>, R> pack(BiFunction<T1, T2, R> func) {
    return pair -> func.apply(pair.getLeft(), pair.getRight());
  }

  public static <A, B> Stream<Pair<A, B>> cartesianProductStream(
      Set<? extends A> a, Set<? extends B> b) {
    return cartesianProductStream(a, b, true);
  }

  public static <A, B> Stream<Pair<A, B>> cartesianProductStream(
      Set<? extends A> a, Set<? extends B> b, boolean parallel) {
    if (parallel)
      return a.parallelStream().flatMap(it -> b.parallelStream().map(it2 -> Pair.of(it, it2)));
    else return a.stream().flatMap(it -> b.stream().map(it2 -> Pair.of(it, it2)));
  }

  public static <T, R> List<R> listMap(Function<? super T, R> func, Iterable<T> os) {
    return StreamSupport.stream(os.spliterator(), false).map(func).collect(Collectors.toList());
  }

  public static <T> T[] prepend(T[] arr, T... es) {
    final T[] newArr = Arrays.copyOf(es, arr.length + es.length);
    System.arraycopy(arr, 0, newArr, es.length, arr.length);
    return newArr;
  }

  public interface SetPartitionEnumerator<T> {
    boolean checkPartition(T x, T y, boolean inSamePartition);

    void beginEnum(int size);

    void beginPartition();

    void endPartition();

    default void enumPartitions(List<T> list) {
      final int[][] bits = partitionBits(list.size());
      beginEnum(bits.length);

      outer:
      for (int[] bit : bits) {
        beginPartition();
        for (int i = 0; i < bit.length; i++)
          for (int j = i + 1; j < bit.length; j++)
            if (!checkPartition(list.get(i), list.get(j), bit[i] == bit[j])) continue outer;
        endPartition();
      }
    }
  }

  public interface MatchEnumerator<T> {
    void beginEnum(int size);

    void beginMatch();

    void endMatch();

    boolean checkMatch(T x, T y);

    default void enumerate(List<T> xs, List<T> ys) {
      beginEnum((int) Math.pow(xs.size(), ys.size()));

      for (List<T> ts : Lists.cartesianProduct(nCopies(ys.size(), xs))) {
        beginMatch();
        for (int i = 0; i < ys.size(); i++) {
          final T x = ts.get(i);
          final T y = ys.get(i);
          if (!checkMatch(x, y)) break;
        }
        endMatch();
      }
    }
  }
}
