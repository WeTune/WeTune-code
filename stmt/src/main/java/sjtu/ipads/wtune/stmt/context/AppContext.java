package sjtu.ipads.wtune.stmt.context;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.AppDao;
import sjtu.ipads.wtune.stmt.dao.SchemaDao;
import sjtu.ipads.wtune.stmt.schema.Schema;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.stmt.statement.Timing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AppContext implements Attrs<AppContext> {
  private static final Map<String, AppContext> KNOWN_APPS = new HashMap<>();

  private String name;
  private String dbType;
  private Schema schema;
  private final Map<String, Schema> alternativeSchemas = new HashMap<>();
  private final Map<Integer, Statement> statements = new HashMap<>();
  public int maxIdSeen = -1;

  public static AppContext of(String name) {
    AppContext app = KNOWN_APPS.get(name);
    if (app != null) return app;

    synchronized (KNOWN_APPS) {
      if ((app = KNOWN_APPS.get(name)) == null) KNOWN_APPS.put(name, app = new AppContext());
    }

    app.setName(name);
    AppDao.instance().inflateOne(app);
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
    if (schema == null) schema = SchemaDao.instance().findOne(name, "base", dbType);
    return schema;
  }

  public List<Timing> timing(String tag) {
    try {
      final Path path = Setup.current().outputDir().resolve(name).resolve("eval." + tag);
      if (!path.toFile().exists()) return Collections.emptyList();

      return Files.lines(path)
          .map(Timing::fromLine)
          .peek(it -> it.setAppName(name).setTag(tag))
          .collect(Collectors.toList());

    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  public List<OutputFingerprint> fingerprints() {
    final Path path = Setup.current().outputDir().resolve(name).resolve("sample");
    if (!path.toFile().exists()) return Collections.emptyList();

    try (final var reader = new BufferedReader(new FileReader(path.toFile()))) {
      final List<OutputFingerprint> fingerprints = new ArrayList<>();
      OutputFingerprint fingerprint;
      while ((fingerprint = OutputFingerprint.readNext(reader)) != null)
        fingerprints.add(fingerprint.setAppName(name));
      return fingerprints;
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public Schema alternativeSchema(String tag) {
    Schema schema = alternativeSchemas.get(tag);
    if (schema == null)
      alternativeSchemas.put(tag, schema = SchemaDao.instance().findOne(name, tag, dbType));
    return schema;
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
