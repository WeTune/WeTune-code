package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.internal.CalCiteStmtProfileImpl;

public interface CalciteStmtProfile {
  String appName();

  int stmtId();

  float p50ImproveQ0();

  float p90ImproveQ0();

  float p99ImproveQ0();

  float p50ImproveQ1();

  float p90ImproveQ1();

  float p99ImproveQ1();

  static CalciteStmtProfile mk(
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
    return new CalCiteStmtProfileImpl(
        appName,
        stmtId,
        p50BaseLatencyQ0,
        p90BaseLatencyQ0,
        p99BaseLatencyQ0,
        p50BaseLatencyQ1,
        p90BaseLatencyQ1,
        p99BaseLatencyQ1,
        p50OptLatency,
        p90OptLatency,
        p99OptLatency);
  }
}
