package sjtu.ipads.wtune.symsolver.utils;

import java.util.Arrays;

public class Partitioner {
  private final int cardinality;
  private final int[][][] partitions;
  private int index;

  public Partitioner(int cardinality) {
    this.cardinality = cardinality;
    this.partitions = partitionsOf(cardinality);
  }

  public void reset() {
    index = 0;
  }

  public int[][] partition() {
    return partitions[index];
  }

  public boolean forward() {
    if (index >= partitions.length - 1) return false;
    ++index;
    return true;
  }

  public int cardinality() {
    return cardinality;
  }

  public int numPartitions() {
    return partitions.length;
  }

  private static final int[] NUM_PARTITIONS = {
    1, 1, 2, 5, 15, 52, 203, 877, 4140, 21147, 115975, 678570, 4213597
  };

  private static final int[][][][] LOOKUP = new int[13][][][];

  static {
    LOOKUP[0] = new int[1][0][0];
    for (int i = 0; i < 12; i++) partitionsOf(i);
  }

  private static int[][][] partitionsOf(int cardinality) {
    final int[][][] existing = LOOKUP[cardinality];
    if (existing != null) return existing;

    final int[][][] base = partitionsOf(cardinality - 1);
    final int[][][] partitions = LOOKUP[cardinality] = new int[NUM_PARTITIONS[cardinality]][][];

    final int element = cardinality - 1;

    int idx = 0;
    for (int[][] basePartition : base)
      for (int i = 0, bound = basePartition.length; i <= bound; i++)
        partitions[idx++] = addElementTo(basePartition, element, i);

    assert idx == NUM_PARTITIONS[cardinality];

    return partitions;
  }

  private static int[][] addElementTo(int[][] target, int element, int idx) {
    final int len = target.length;
    final int index = Math.min(idx, len);
    final int[][] ret = Arrays.copyOf(target, index >= len ? len + 1 : len);

    ret[index] = addElementTo(ret[index], element);
    return ret;
  }

  private static int[] addElementTo(int[] target, int element) {
    if (target == null) return new int[] {element};
    else {
      final int len = target.length;
      final int[] newArr = Arrays.copyOf(target, len + 1);
      newArr[len] = element;
      return newArr;
    }
  }
}
