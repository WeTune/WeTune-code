package sjtu.ipads.wtune.testbed.util;

import static sjtu.ipads.wtune.testbed.util.MathHelper.pow10Factor;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import java.util.NavigableMap;
import java.util.TreeMap;

public abstract class RandomHelper {
  public static int GLOBAL_SEED = 0x98761234;

  private static final long MULTIPLIER = 0x5DEECE66DL;
  private static final long ADDEND = 0xBL;
  private static final long MASK = (1L << 48) - 1;
  private static final double DOUBLE_UNIT = 0x1.0p-53; // 1.0 / (1L << 53)

  private static int hash(int x) {
    // https://stackoverflow.com/a/12996028
    x = ((x >>> 16) ^ x) * 0x45d9f3b;
    x = ((x >>> 16) ^ x) * 0x45d9f3b;
    x = (x >>> 16) ^ x;
    return x;
  }

  private static int uniform(int seed, int bits) {
    return (int) (((seed * MULTIPLIER + ADDEND) & MASK) >>> (48 - bits));
  }

  public static int uniformRandomInt(int seed) {
    return uniform(hash(seed), 32) & (~Integer.MIN_VALUE);
  }

  public static double uniformRandomDouble(int seed) {
    seed = hash(seed);
    return (((long) (uniform(seed, 26)) << 27) + uniform(seed, 27)) * DOUBLE_UNIT;
  }

  public static int uniqueRandomIntBin(int seed, int index, int bits) {
    // here `seed` needn't to be hash
    if (index < 0 || index >= (1 << bits))
      throw new IllegalArgumentException("impossible to generate unique random integer");

    final int half1 = bits / 2;
    final int half2 = (bits + 1) / 2;
    final int mask1 = (1 << half1) - 1;
    final int mask2 = (1 << half2) - 1;

    for (int round = 0; round < 5; ++round) {
      final int mod = ((index >> half1) << 4) + round;
      index ^= (uniformRandomInt(seed + mod) & mask1);
      index = ((index & mask2) << half1) | ((index >> half2) & mask1);
    }
    return index;
  }

  public static int uniqueRandomIntDec(int seed, int index, int digits) {
    // here `seed` needn't to be hash
    if (index < 0 || digits > 9 || index >= MathHelper.pow10(digits))
      throw new IllegalArgumentException("impossible to generate unique random integer");

    final int[] factors = pow10Factor(digits);
    final int firstFactor = factors[0], secondFactor = factors[1];

    for (int round = 0; round < 5; ++round) {
      final int left = index / secondFactor;
      final int right = index % secondFactor;

      index = firstFactor * right + ((left + uniformRandomInt(seed + right + round)) % firstFactor);
    }
    return index;
  }

  public static RandGen makeUniformRand() {
    return RandomHelper::uniformRandomInt;
  }

  public static RandGen makeZipfRand(int skew) {
    return new ZipfRand(skew);
  }

  private static class ZipfRand implements RandGen {
    private static final double EPSILON = 1E-3;
    private final NavigableMap<Double, Integer> histogram;

    private ZipfRand(double skew) {
      histogram = makeHistogram(skew);
    }

    private static NavigableMap<Double, Integer> makeHistogram(double skew) {
      final NavigableMap<Double, Integer> map = new TreeMap<>();
      final TDoubleList bars = new TDoubleArrayList();
      bars.add(1.0);

      double sum = 1.0;
      for (int i = 2; ; i++) {
        final double e = 1 / Math.pow(i, skew);
        sum += e;
        if (e / sum < EPSILON) break;
        bars.add(e);
      }

      double acc = 0;
      int i = 0;
      System.out.println(bars.size());
      for (int bound = bars.size(); i < bound; i++) {
        final double p = bars.get(i) / sum;
        acc += p;
        map.put(acc, i);
      }
      map.put(1.0, i);

      return map;
    }

    @Override
    public int random(int seed) {
      return histogram.ceilingEntry(uniformRandomDouble(seed)).getValue();
    }
  }
}
