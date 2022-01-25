package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.stmt.CalciteStmtProfile;

public class CalCiteStmtProfileImpl implements CalciteStmtProfile {
  private final String appName;
  private final int stmtId;

  private final int p50BaseLatencyQ0;
  private final int p90BaseLatencyQ0;
  private final int p99BaseLatencyQ0;

  private final int p50BaseLatencyQ1;
  private final int p90BaseLatencyQ1;
  private final int p99BaseLatencyQ1;

  private final int p50OptLatency;
  private final int p90OptLatency;
  private final int p99OptLatency;

  public CalCiteStmtProfileImpl(
      String appName,
      int stmtId,
      int p50BaseLatencyQ0,
      int p90BaseLatencyQ0,
      int p99BaseLatencyQ0,
      int p50BaseLatencyQ1,
      int p90BaseLatencyQ1,
      int p99BaseLatencyQ1,
      int p50OptLatency,
      int p90OptLatency,
      int p99OptLatency) {
    this.appName = appName;
    this.stmtId = stmtId;
    this.p50BaseLatencyQ0 = p50BaseLatencyQ0;
    this.p90BaseLatencyQ0 = p90BaseLatencyQ0;
    this.p99BaseLatencyQ0 = p99BaseLatencyQ0;
    this.p50BaseLatencyQ1 = p50BaseLatencyQ1;
    this.p90BaseLatencyQ1 = p90BaseLatencyQ1;
    this.p99BaseLatencyQ1 = p99BaseLatencyQ1;
    this.p50OptLatency = p50OptLatency;
    this.p90OptLatency = p90OptLatency;
    this.p99OptLatency = p99OptLatency;
  }

  @Override
  public String appName() {
    return appName;
  }

  @Override
  public int stmtId() {
    return stmtId;
  }

  @Override
  public float p50ImproveQ0() {
    return (float) (1.0 - ((float) p50OptLatency) / ((float) p50BaseLatencyQ0));
  }

  @Override
  public float p90ImproveQ0() {
    return (float) (1.0 - ((float) p90OptLatency) / ((float) p90BaseLatencyQ0));
  }

  @Override
  public float p99ImproveQ0() {
    return (float) (1.0 - ((float) p99OptLatency) / ((float) p99BaseLatencyQ0));
  }

  @Override
  public float p50ImproveQ1() {
    return (float) (1.0 - ((float) p50OptLatency) / ((float) p50BaseLatencyQ1));
  }

  @Override
  public float p90ImproveQ1() {
    return (float) (1.0 - ((float) p50OptLatency) / ((float) p90BaseLatencyQ1));
  }

  @Override
  public float p99ImproveQ1() {
    return (float) (1.0 - ((float) p50OptLatency) / ((float) p99BaseLatencyQ1));
  }
}
