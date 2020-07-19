package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.FingerprintDaoInstance;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public interface FingerprintDao {
  List<OutputFingerprint> findByStmt(Statement stmt);

  void save(OutputFingerprint fingerprint);

  default void registerAsGlobal() {
    FingerprintDaoInstance.register(this);
  }
}
