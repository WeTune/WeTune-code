package sjtu.ipads.wtune.stmt.context;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.stmt.dao.internal.AppDaoInstance;
import sjtu.ipads.wtune.stmt.dao.internal.SchemaDaoInstance;
import sjtu.ipads.wtune.stmt.schema.Schema;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AppContext implements Attrs<AppContext> {
  private static final Map<String, AppContext> KNOWN_APPS = new HashMap<>();

  private String name;
  private String dbType;
  private Schema schema;
  private Map<Integer, Statement> statements = new HashMap<>();
  public int maxIdSeen = -1;

  public static AppContext of(String name) {
    AppContext app = KNOWN_APPS.get(name);
    if (app != null) return app;

    synchronized (KNOWN_APPS) {
      if ((app = KNOWN_APPS.get(name)) == null) KNOWN_APPS.put(name, app = new AppContext());
    }

    app.setName(name);
    AppDaoInstance.inflateOne(app);
    return app;
  }

  public static Collection<AppContext> all() {
    return KNOWN_APPS.values();
  }

  public String name() {
    return name;
  }

  public String dbType() {
    return dbType;
  }

  public Map<Integer, Statement> statements() {
    return statements;
  }

  public Schema schema() {
    if (schema == null) schema = SchemaDaoInstance.findOne(name, dbType);
    return schema;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public void setDbType(String dbType) {
    this.dbType = dbType;
  }

  public boolean addStatement(Statement stmt) {
    if (stmt.stmtId() == -1) stmt.setStmtId(maxIdSeen + 1);
    maxIdSeen = stmt.stmtId();
    return statements.put(stmt.stmtId(), stmt) == stmt;
  }

  public void removeStatement(Statement stmt) {
    statements.remove(stmt.stmtId());
  }

  private final Map<String, Object> directAttrs = new HashMap<>();

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }
}
