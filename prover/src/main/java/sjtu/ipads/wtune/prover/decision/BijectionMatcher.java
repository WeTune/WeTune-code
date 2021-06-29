package sjtu.ipads.wtune.prover.decision;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import sjtu.ipads.wtune.prover.utils.Util;

abstract class BijectionMatcher<T> {
  protected final List<T> xs, ys;

  private List<T> matching;

  protected BijectionMatcher(List<T> xs, List<T> ys) {
    this.xs = xs;
    this.ys = ys;
  }

  List<T> matching() {
    return matching;
  }

  boolean match() {
    if (xs.size() != ys.size()) return false;

    final int[] matched = new int[xs.size()];
    Arrays.fill(matched, -1);

    return match0(0, matched);
  }

  private boolean match0(int i, int[] matched) {
    if (i >= matched.length) {
      final List<T> matching = Util.arrange(ys, matched);
      if (onMatched(xs, matching)) {
        this.matching = matching;
        return true;
      } else return false;
    }

    final T x = xs.get(i);
    for (int j = 0, bound = matched.length; j < bound; ++j) {
      if (matched[j] >= 0) continue; // skip taken slot
      if (!tryMatch(x, ys.get(j))) continue; // if x[i] and y[j] not match, continue
      // recursion + backtrace
      matched[j] = i; // mark that x[i] and y[j] has matched
      final boolean successful = match0(i + 1, matched); // recursion: check the remaining
      matched[j] = -1;
      // backtrace: if failed, clear the mark and try next.
      if (successful) return true;
      else revokeMatch();
    }

    return false;
  }

  protected void revokeMatch() {}

  protected abstract boolean tryMatch(T x, T y);

  protected boolean onMatched(List<T> xs, List<T> ys) {
    return true;
  }

  static <T> BijectionMatcher<T> makeBijectionMatcher(List<T> xs, List<T> ys, BiPredicate<T, T> func) {
    return new BijectionMatcher<T>(xs, ys) {
      @Override
      protected boolean tryMatch(T x, T y) {
        return func.test(x, y);
      }
    };
  }
}
