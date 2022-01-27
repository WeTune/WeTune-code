package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.stmt.CalciteStmtProfile;

public class CalCiteStmtProfileImpl implements CalciteStmtProfile {
  private static final float ERR_PROFILE = -1.0f;

  private final String appName;
  private final int stmtId;

  private final int p50BaseLatency;
  private final int p90BaseLatency;
  private final int p99BaseLatency;

  private final int p50OptLatencyCalcite;
  private final int p90OptLatencyCalcite;
  private final int p99OptLatencyCalcite;

  private final int p50OptLatencyWeTune;
  private final int p90OptLatencyWeTune;
  private final int p99OptLatencyWeTune;

  public CalCiteStmtProfileImpl(
      String appName,
      int stmtId,
      int p50BaseLatency,
      int p90BaseLatency,
      int p99BaseLatency,
      int p50OptLatencyCalcite,
      int p90OptLatencyCalcite,
      int p99OptLatencyCalcite,
      int p50OptLatencyWeTune,
      int p90OptLatencyWeTune,
      int p99OptLatencyWeTune) {
    this.appName = appName;
    this.stmtId = stmtId;
    this.p50BaseLatency = p50BaseLatency;
    this.p90BaseLatency = p90BaseLatency;
    this.p99BaseLatency = p99BaseLatency;
    this.p50OptLatencyCalcite = p50OptLatencyCalcite;
    this.p90OptLatencyCalcite = p90OptLatencyCalcite;
    this.p99OptLatencyCalcite = p99OptLatencyCalcite;
    this.p50OptLatencyWeTune = p50OptLatencyWeTune;
    this.p90OptLatencyWeTune = p90OptLatencyWeTune;
    this.p99OptLatencyWeTune = p99OptLatencyWeTune;
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
  public float p50ImproveCalcite() {
    if (p50OptLatencyCalcite < 0) return ERR_PROFILE;
    return (float) (1.0 - ((float) p50OptLatencyCalcite) / ((float) p50BaseLatency));
  }

  @Override
  public float p90ImproveCalcite() {
    if (p90OptLatencyCalcite < 0) return ERR_PROFILE;
    return (float) (1.0 - ((float) p90OptLatencyCalcite) / ((float) p90BaseLatency));
  }

  @Override
  public float p99ImproveCalcite() {
    if (p99OptLatencyCalcite < 0) return ERR_PROFILE;
    return (float) (1.0 - ((float) p99OptLatencyCalcite) / ((float) p99BaseLatency));
  }

  @Override
  public float p50ImproveWeTune() {
    if (p50OptLatencyWeTune < 0) return ERR_PROFILE;
    return (float) (1.0 - ((float) p50OptLatencyWeTune) / ((float) p50BaseLatency));
  }

  @Override
  public float p90ImproveWeTune() {
    if (p90OptLatencyWeTune < 0) return ERR_PROFILE;
    return (float) (1.0 - ((float) p90OptLatencyWeTune) / ((float) p90BaseLatency));
  }

  @Override
  public float p99ImproveWeTune() {
    if (p99OptLatencyWeTune < 0) return ERR_PROFILE;
    return (float) (1.0 - ((float) p99OptLatencyWeTune) / ((float) p99BaseLatency));
  }

}
