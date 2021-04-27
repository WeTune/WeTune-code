package sjtu.ipads.wtune.testbed.profile;

public interface Metric {
  void addRecord(long latency);

  long atPercentile(double percentile);

  static Metric make(int nExpectedRecords) {
    return new MetricImpl(nExpectedRecords);
  }
}
