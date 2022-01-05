package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.schema.Column;
import sjtu.ipads.wtune.sql.schema.Column.Flag;
import sjtu.ipads.wtune.sql.schema.Constraint;
import sjtu.ipads.wtune.sql.schema.SchemaPatch;
import sjtu.ipads.wtune.sql.schema.SchemaPatch.Type;
import sjtu.ipads.wtune.sql.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.dao.TimingDao;
import sjtu.ipads.wtune.stmt.rawlog.RawLog;
import sjtu.ipads.wtune.stmt.rawlog.RawStmt;
import sjtu.ipads.wtune.stmt.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sql.schema.SchemaPatch.Type.FOREIGN_KEY;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.installParamMarkers;

public interface Workflow {
  static void inferForeignKeys(String appName) {
    final Map<Column, Column> inferred = new HashMap<>();
    for (Statement statement : Statement.findByApp(appName)) {
      final SqlNode ast = statement.ast();
      ast.context().setSchema(statement.app().schema("base", true));
      //      normalize(ast);
      inferred.putAll(InferForeignKey.analyze(statement.ast()));
    }
    final SchemaPatchDao dao = SchemaPatchDao.instance();
    dao.beginBatch();
    for (var pair : inferred.entrySet()) {
      final Column referee = pair.getKey();
      final Column referred = pair.getValue();
      final SchemaPatch patch =
          SchemaPatch.build(
              FOREIGN_KEY,
              appName,
              referee.tableName(),
              singletonList(referee.name()),
              referred.tableName() + "." + referred.name());
      dao.save(patch);
    }
    dao.endBatch();
    //    inferred.entrySet().forEach(System.out::println);
    //    System.out.println(inferred.size());
    //        inferred.forEach(System.out::println);
  }

  static void inferNotNull(String appName) {
    final SchemaPatchDao dao = SchemaPatchDao.instance();

    dao.beginBatch();
    for (Table table : App.of(appName).schema("base", true).tables())
      for (Constraint constraint : table.constraints())
        for (Column column : constraint.columns()) {
          if (column.isFlag(Flag.NOT_NULL)) continue;
          final SchemaPatch patch =
              SchemaPatch.build(
                  Type.NOT_NULL, appName, table.name(), singletonList(column.name()), null);
          dao.save(patch);
        }
    dao.endBatch();
  }

  static void loadTiming(String appName, String tag) {
    final Stream<String> records = FileUtils.readLines("timing", appName + "." + tag + ".timing");
    Timing.fromLines(appName, tag, records).forEach(TimingDao.instance()::save);
  }

  static void loadSQL(String appName, Path logPath, Path tracePath, int rangeStart, int rangeEnd)
      throws IOException {
    final RawLog logs = RawLog.open(appName, logPath, tracePath).skip(rangeStart);
    final int total = rangeEnd - rangeStart;

    final StatementDao dao = StatementDao.instance();
    final List<Statement> existing = dao.findByApp(appName);

    final Set<String> keys = new HashSet<>();
    for (Statement stmt : existing) {
      final SqlNode ast = stmt.ast();
      installParamMarkers(ast);
      keys.add(ast.toString());
    }

    int nextId = maxId(existing);
    int count = 0, added = 0;

    dao.beginBatch();

    for (RawStmt log : logs) {
      ++count;
      if (count > total) break;
      if (count % 1000 == 0) System.out.println("~ " + count);

      final String sql = log.sql();
      if (!sql.startsWith("select") && !sql.startsWith("SELECT")) continue;

      final String stackTrace = log.stackTrace() == null ? "" : log.stackTrace().toString();
      final Statement stmt = Statement.mk(appName, sql, stackTrace);
      final SqlContext ctx = stmt.ast().context();
      ctx.setSchema(stmt.app().schema("base"));
      installParamMarkers(stmt.ast());
      final String key = stmt.ast().toString();

      if (keys.add(key)) {
        stmt.setStmtId(++nextId);
        dao.save(stmt);
        ++added;
        if (added % 100 == 0) {
          dao.endBatch();
          dao.beginBatch();
        }
      }
    }

    dao.endBatch();
    logs.close();

    System.out.println(added + " statements added to " + appName);
  }

  private static int maxId(List<Statement> stmts) {
    return stmts.stream().mapToInt(Statement::stmtId).max().orElse(0);
  }
}
