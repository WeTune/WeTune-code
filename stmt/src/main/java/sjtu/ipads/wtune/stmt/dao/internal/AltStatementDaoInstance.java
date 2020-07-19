package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.AltStatementDao;
import sjtu.ipads.wtune.stmt.statement.AltStatement;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public class AltStatementDaoInstance {
  private static AltStatementDao INSTANCE;

  public static void register(AltStatementDao instance) {
    INSTANCE = instance;
  }

  private static AltStatementDao instance0() {
    if (INSTANCE == null) {
      throw new StmtException("no global alt stmt dao");
    }

    return INSTANCE;
  }

  public static AltStatementDao instance() {
    return INSTANCE;
  }

  public static List<AltStatement> findByStmt(Statement stmt) {
    return instance0().findByStmt(stmt);
  }
}
