package sjtu.ipads.wtune.prover.decision;

import java.util.List;

abstract class Matcher<T> {
  private final List<T> xs, ys;

  protected Matcher(List<T> xs, List<T> ys) {
    this.xs = xs;
    this.ys = ys;
  }

  boolean match() {
    final boolean[] matchedY = new boolean[ys.size()]; // save some repeated calculation

    outer:
    for (T x : xs) {
      for (int i = 0, bound = ys.size(); i < bound; ++i)
        if (tryMatch(x, ys.get(i))) {
          matchedY[i] = true;
          continue outer;
        }
      return false;
    }

    for (int i = 0, bound = ys.size(); i < bound; ++i) {
      if (matchedY[i]) continue;
      final T y = ys.get(i);
      if (xs.stream().noneMatch(x -> tryMatch(x, y))) return false;
    }

    return true;
  }

  abstract boolean tryMatch(T x, T y);
}
