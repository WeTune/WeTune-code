package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.internal.CalCiteStmtProfileImpl;

public interface CalciteStmtProfile {
  String appName();

  int stmtId();

  float p50ImproveCalcite();

  float p90ImproveCalcite();

  float p99ImproveCalcite();

  float p50ImproveWeTune();

  float p90ImproveWeTune();

  float p99ImproveWeTune();

  static CalciteStmtProfile mk(
      String appName,
      int stmtId,
      int p50Base,
      int p90Base,
      int p99Base,
      int p50OptCalcite,
      int p90OptCalcite,
      int p99OptCalcite,
      int p50OptWeTune,
      int p90OptWeTune,
      int p99OptWeTune) {
    return new CalCiteStmtProfileImpl(
        appName,
        stmtId,
        p50Base,
        p90Base,
        p99Base,
        p50OptCalcite,
        p90OptCalcite,
        p99OptCalcite,
        p50OptWeTune,
        p90OptWeTune,
        p99OptWeTune);
  }
}
