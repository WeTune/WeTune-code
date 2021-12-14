package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.IntMetric;
import sjtu.ipads.wtune.common.utils.LongMetric;
import sjtu.ipads.wtune.common.utils.Metric;
import sjtu.ipads.wtune.common.utils.Metrics;

import java.util.List;

class EnumerationMetrics implements Metrics<EnumerationMetrics>, AutoCloseable {
  final IntMetric numEnumeratorInvocations = new IntMetric("#Enum", 1);
  final IntMetric numEnumeratedConstraintSets = new IntMetric("#Constraints");
  final IntMetric numProverInvocations = new IntMetric("#Prover");
  final IntMetric numCacheHitEq = new IntMetric("#CacheEq");
  final IntMetric numCacheHitNeq = new IntMetric("#CacheNeq");
  final IntMetric numEq = new IntMetric("#Eq");
  final IntMetric numNeq = new IntMetric("#Neq");
  final IntMetric numUnknown = new IntMetric("#Unknown");
  final LongMetric elapsedEnum = new LongMetric("Enum(ms)");
  final LongMetric elapsedEq = new LongMetric("Eq(ms)");
  final LongMetric elapsedNeq = new LongMetric("Neq(ms)");
  final LongMetric elapsedUnknown = new LongMetric("Unknown(ms)");

  private final List<Metric> metrics =
      List.of(
          numEnumeratorInvocations,
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
          elapsedUnknown);

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
