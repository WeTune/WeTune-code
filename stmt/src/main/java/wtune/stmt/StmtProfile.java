package wtune.stmt;

import wtune.stmt.internal.StmtProfileImpl;

public interface StmtProfile {
  String appName();

  int stmtId();

  float p50Improve();

  float p90Improve();

  float p99Improve();

  static StmtProfile mk(String appName,
                        int stmtId,
                        int p50BaseLatency,
                        int p90BaseLatency,
                        int p99BaseLatency,
                        int p50OptLatency,
                        int p90OptLatency,
                        int p99OptLatency) {
    return new StmtProfileImpl(
        appName,
        stmtId,
        p50BaseLatency,
        p90BaseLatency,
        p99BaseLatency,
        p50OptLatency,
        p90OptLatency,
        p99OptLatency);
  }
}
