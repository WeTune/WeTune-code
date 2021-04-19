package sjtu.ipads.wtune.testbed.util;

import java.util.Arrays;

public interface MathHelper {
  int[] POW_10 = {
    1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000,
  };

  int[][] POW_10_FACTOR = {
    {1, 1},
    {2, 5},
    {10, 10},
    {25, 40},
    {100, 100},
    {250, 400},
    {1000, 1000},
    {2500, 4000},
    {10000, 10000},
    {25000, 40000}
  };

  static int pow10(int exp) {
    if (exp >= 10) throw new IllegalArgumentException();
    return POW_10[exp];
  }

  static int[] pow10Factor(int power) {
    if (power >= 10) throw new IllegalArgumentException();
    return POW_10_FACTOR[power];
  }

  static int base10(int power) {
    return Arrays.binarySearch(POW_10, power);
  }

  static boolean isPow2(int i) {
    return ((i - 1) & i) == 0;
  }

  static boolean isPow10(int i) {
    return base10(i) >= 0;
  }

  static int ceilingPow2(int v) {
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    v++;
    return v;
  }
}
