package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.internal.CalCiteStmtProfileImpl;

public interface CalciteStmtProfile {
  String appName();

  int stmtId();

  Float p50ImproveCalcite();

  Float p90ImproveCalcite();

  Float p99ImproveCalcite();

  Float p50ImproveWeTune();

  Float p90ImproveWeTune();

  Float p99ImproveWeTune();

  static CalciteStmtProfile mk(
      String appName,
      int stmtId,
      Integer p50Base,
      Integer p90Base,
      Integer p99Base,
      Integer p50OptCalcite,
      Integer p90OptCalcite,
      Integer p99OptCalcite,
      Integer p50OptWeTune,
      Integer p90OptWeTune,
      Integer p99OptWeTune) {
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
