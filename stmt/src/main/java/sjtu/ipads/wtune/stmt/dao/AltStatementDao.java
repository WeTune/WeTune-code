package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.AltStatementDaoInstance;
import sjtu.ipads.wtune.stmt.statement.AltStatement;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public interface AltStatementDao {
  List<AltStatement> findByStmt(Statement stmt);

  default void registerAsGlobal() {
    AltStatementDaoInstance.register(this);
  }
}
