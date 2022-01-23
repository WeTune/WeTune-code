package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.stmt.StmtProfile;

public class StmtProfileImpl implements StmtProfile {
  private final String appName;
  private final int stmtId;

  private final int p50BaseLatency;
  private final int p90BaseLatency;
  private final int p99BaseLatency;

  private final int p50OptLatency;
  private final int p90OptLatency;
  private final int p99OptLatency;

  public StmtProfileImpl(
      String appName,
      int stmtId,
      int p50BaseLatency,
      int p90BaseLatency,
      int p99BaseLatency,
      int p50OptLatency,
      int p90OptLatency,
      int p99OptLatency) {
    this.appName = appName;
    this.stmtId = stmtId;
    this.p50BaseLatency = p50BaseLatency;
    this.p90BaseLatency = p90BaseLatency;
    this.p99BaseLatency = p99BaseLatency;
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
  public int p50BaseLatency() {
    return p50BaseLatency;
  }

  @Override
  public int p90BaseLatency() {
    return p90BaseLatency;
  }

  @Override
  public int p99BaseLatency() {
    return p99BaseLatency;
  }

  @Override
  public int p50OptLatency() {
    return p50OptLatency;
  }

  @Override
  public int p90OptLatency() {
    return p90OptLatency;
  }

  @Override
  public int p99OptLatency() {
    return p99OptLatency;
  }

  @Override
  public float p50Improve() {
    return ((float) (1.0 - (float) p50OptLatency) / ((float) p50BaseLatency));
  }

  @Override
  public float p90Improve() {
    return ((float) (1.0 - (float) p90OptLatency) / ((float) p90BaseLatency));
  }

  @Override
  public float p99Improve() {
    return ((float) (1.0 - (float) p99OptLatency) / ((float) p99BaseLatency));
  }
}
