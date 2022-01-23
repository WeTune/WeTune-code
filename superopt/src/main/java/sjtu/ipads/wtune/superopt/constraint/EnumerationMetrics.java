package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.*;

import java.util.List;

public class EnumerationMetrics implements Metrics<EnumerationMetrics>, AutoCloseable {
  public final IntMetric numEnumeratorInvocations = new IntMetric("#Enum", 1);
  public final IntMetric numTotalConstraintSets = new IntMetric("#TotalConstraints");
  public final IntMetric numEnumeratedConstraintSets = new IntMetric("#EnumConstraints");
  public final IntMetric numProverInvocations = new IntMetric("#Prover");
  public final IntMetric numCacheHitEq = new IntMetric("#CacheEq");
  public final IntMetric numCacheHitNeq = new IntMetric("#CacheNeq");
  public final IntMetric numEq = new IntMetric("#Eq");
  public final IntMetric numNeq = new IntMetric("#Neq");
  public final IntMetric numUnknown = new IntMetric("#Unknown");
  public final LongMetric elapsedEnum = new LongMetric("Enum(ms)");
  public final LongMetric elapsedEq = new LongMetric("Eq(ms)");
  public final LongMetric elapsedNeq = new LongMetric("Neq(ms)");
  public final LongMetric elapsedUnknown = new LongMetric("Unknown(ms)");
  public final IntMetric uncertain = new IntMetric("#Uncertain");

  private final List<Metric> metrics =
      List.of(
          numEnumeratorInvocations,
          numTotalConstraintSets,
          numEnumeratedConstraintSets,
          numProverInvocations,
          numCacheHitEq,
          numCacheHitNeq,
          numEq,
          numNeq,
          numUnknown,
          elapsedEnum,
          elapsedEq,
          elapsedNeq,
          elapsedUnknown,
          uncertain);

  static EnumerationMetrics open() {
    return EnumerationMetricsContext.instance().local(true);
  }

  static EnumerationMetrics current() {
    return EnumerationMetricsContext.instance().local(false);
  }

  public void close() {
    EnumerationMetricsContext.instance().updateGlobal();
  }

  @Override
  public List<Metric> metrics() {
    return metrics;
  }

  @Override
  public String toString() {
    return stringify(new StringBuilder()).toString();
  }
}
