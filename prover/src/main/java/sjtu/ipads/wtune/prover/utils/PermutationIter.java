package sjtu.ipads.wtune.prover.utils;

import java.util.Arrays;
import java.util.Iterator;

public class PermutationIter implements Iterator<int[]> {
  private final int n, k;
  private final int[] buffer;
  private final boolean[] masks;

  private boolean isDrained;

  private PermutationIter(int n, int k) {
    this.n = n;
    this.k = k;
    this.buffer = new int[k];
    this.masks = new boolean[n];
    this.isDrained = true;

    for (int i = 0; i < k; i++) {
      buffer[i] = i;
      masks[i] = true;
    }
  }

  private boolean forward(int idx) {
    final int original = buffer[idx];
    masks[original] = false; // clear the mask first

    // find next available slot
    int slot = findNextSlot(original + 1);

    if (slot >= n && idx == 0) {
      return false; // this seq of permutation has been drained
    } else {
      if (slot >= n) {
        if (!forward(idx - 1)) return false;
        slot = findNextSlot(0);
      }
      buffer[idx] = slot;
      masks[slot] = true;
      return true;
    }
  }

  private int findNextSlot(final int start) {
    int bit = start;
    while (bit < n && masks[bit]) ++bit;
    return bit;
  }

  public static Iterable<int[]> permute(int n, int k) {
    if (n < 0) throw new IllegalArgumentException("negative #element " + k);
    if (k < 0) throw new IllegalArgumentException("negative #choice " + k);
    if (k > n)
      throw new IllegalArgumentException("#choice %d is greater than #elements %d".formatted(k, n));
    return () -> new PermutationIter(n, k);
  }

  @Override
  public boolean hasNext() {
    return isDrained;
  }

  @Override
  public int[] next() {
    final int[] ret = Arrays.copyOf(buffer, buffer.length);
    isDrained = forward(buffer.length - 1);
    return ret;
  }
}
