package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.StatementDao;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public class StatementDaoInstance {
  private static StatementDao INSTANCE;

  public static void register(StatementDao instance) {
    INSTANCE = instance;
  }

  private static StatementDao instance0() {
    if (INSTANCE == null) {
      throw new StmtException("no global statement dao");
    }

    return INSTANCE;
  }

  public static StatementDao instance() {
    return INSTANCE;
  }

  public static Statement findOne(String appName, int stmtId) {
    return INSTANCE.findOne(appName, stmtId);
  }

  public static List<Statement> findByApp(String appName) {
    return instance0().findByApp(appName);
  }

  public static List<Statement> findAll() {
    return instance0().findAll();
  }

  public static void close() {
    instance0().close();
  }
}
