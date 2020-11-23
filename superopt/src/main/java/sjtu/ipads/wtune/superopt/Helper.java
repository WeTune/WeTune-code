package sjtu.ipads.wtune.superopt;

import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

  public static <T> List<T> concat(List<T> left, List<T> right) {
    final List<T> t = new ArrayList<>();
    t.addAll(left);
    t.addAll(right);
    return t;
  }

  public static <T1, T2, R> Function<Pair<T1, T2>, R> pack(BiFunction<T1, T2, R> func) {
    return pair -> func.apply(pair.getLeft(), pair.getRight());
  }

  public static <A, B, C> Stream<C> zip(
      Stream<? extends A> a,
      Stream<? extends B> b,
      BiFunction<? super A, ? super B, ? extends C> zipper) {
    Objects.requireNonNull(zipper);
    Spliterator<? extends A> aSpliterator = Objects.requireNonNull(a).spliterator();
    Spliterator<? extends B> bSpliterator = Objects.requireNonNull(b).spliterator();

    // Zipping looses DISTINCT and SORTED characteristics
    int characteristics =
        aSpliterator.characteristics()
            & bSpliterator.characteristics()
            & ~(Spliterator.DISTINCT | Spliterator.SORTED);

    long zipSize =
        ((characteristics & Spliterator.SIZED) != 0)
            ? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown())
            : -1;

    Iterator<A> aIterator = Spliterators.iterator(aSpliterator);
    Iterator<B> bIterator = Spliterators.iterator(bSpliterator);
    Iterator<C> cIterator =
        new Iterator<C>() {
          @Override
          public boolean hasNext() {
            return aIterator.hasNext() && bIterator.hasNext();
          }

          @Override
          public C next() {
            return zipper.apply(aIterator.next(), bIterator.next());
          }
        };

    Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
    return (a.isParallel() || b.isParallel())
        ? StreamSupport.stream(split, true)
        : StreamSupport.stream(split, false);
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

  public static <T> List<T> safeAdd(List<T> t, T obj) {
    if (obj != null) t.add(obj);
    return t;
  }
}
