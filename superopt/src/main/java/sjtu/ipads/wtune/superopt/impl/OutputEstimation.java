package sjtu.ipads.wtune.superopt.impl;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.operators.Proj;

import java.util.ArrayList;
import java.util.List;

public class OutputEstimation {
  private final int minSourceInputs;
  private final int maxSourceInputs;
  private final int hasAggColumn;

  private static final OutputEstimation[][][] CACHE = new OutputEstimation[5][5][3];
  private static final List<OutputEstimation> ALL = new ArrayList<>(30);

  static {
    for (int i = 0; i < 5; i++)
      for (int j = i; j < 5; j++)
        for (int k = 0; k < 3; k++) ALL.add(CACHE[i][j][k] = new OutputEstimation(i + 1, j + 1, k));
  }

  private OutputEstimation(int minSourceInputs, int maxSourceInputs, int hasAggColumn) {
    this.minSourceInputs = minSourceInputs;
    this.maxSourceInputs = maxSourceInputs;
    this.hasAggColumn = hasAggColumn;
  }

  private static OutputEstimation getCache(
      int minSourceInputs, int maxSourceInputs, int hasAggColumn) {
    return CACHE[minSourceInputs - 1][maxSourceInputs - 1][hasAggColumn];
  }

  public static OutputEstimation init() {
    return getCache(1, 1, 0);
  }

  public static OutputEstimation of(Agg ignored, OutputEstimation input) {
    return getCache(input.minSourceInputs, input.maxSourceInputs, 2);
  }

  public static OutputEstimation of(Proj ignored, OutputEstimation input) {
    return getCache(1, input.maxSourceInputs, input.hasAggColumn == 0 ? 0 : 1);
  }

  public static OutputEstimation of(Join ignored, OutputEstimation left, OutputEstimation right) {
    return getCache(
        left.minSourceInputs + right.minSourceInputs,
        left.maxSourceInputs + right.maxSourceInputs,
        (left.hasAggColumn == 2 || right.hasAggColumn == 2)
            ? 2
            : (left.hasAggColumn == 0 && right.hasAggColumn == 0 ? 0 : 1));
  }

  public boolean mayMatch(OutputEstimation other) {
    return this.minSourceInputs <= other.maxSourceInputs
        && this.maxSourceInputs >= other.minSourceInputs
        && (!(this.hasAggColumn * other.hasAggColumn == 0
            && this.hasAggColumn + other.hasAggColumn == 2));
  }

  public static List<Pair<OutputEstimation, OutputEstimation>> pairsMayMatch() {
    final List<Pair<OutputEstimation, OutputEstimation>> ret = new ArrayList<>(435);
    for (int i = 0; i < ALL.size(); i++)
      for (int j = i + 1; j < ALL.size(); j++) {
        final OutputEstimation e1 = ALL.get(i), e2 = ALL.get(j);
        if (e1.mayMatch(e2)) ret.add(Pair.of(e1, e2));
      }
    return ret;
  }

  @Override
  public String toString() {
    return "OutputEstimation{"
        + minSourceInputs
        + ", "
        + maxSourceInputs
        + ", "
        + hasAggColumn
        + '}';
  }
}
