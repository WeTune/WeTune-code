package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.FingerprintDao;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public class FingerprintDaoInstance {
  private static FingerprintDao INSTANCE;

  public static void register(FingerprintDao instance) {
    INSTANCE = instance;
  }

  private static FingerprintDao instance0() {
    if (INSTANCE == null) {
      throw new StmtException("no global schema dao");
    }

    return INSTANCE;
  }

  public static FingerprintDao instance() {
    return INSTANCE;
  }

  public static List<OutputFingerprint> findByStmt(Statement stmt) {
    return instance0().findByStmt(stmt);
  }

  public static void save(OutputFingerprint fingerprint) {
    instance0().save(fingerprint);
  }
}
