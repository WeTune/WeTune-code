package sjtu.ipads.wtune.stmt.support;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.schema.SchemaPatch.Type.FOREIGN_KEY;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.clean;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeBool;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeConstantTable;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeJoinCondition;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeParam;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeTuple;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveBoolExpr;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveParamFull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Column.Flag;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch.Type;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.dao.TimingDao;
import sjtu.ipads.wtune.stmt.rawlog.RawLog;
import sjtu.ipads.wtune.stmt.rawlog.RawStmt;
import sjtu.ipads.wtune.stmt.utils.FileUtils;

public interface Workflow {
  static void inferForeignKeys(String appName) {
    final Map<Column, Column> inferred = new HashMap<>();
    for (Statement statement : Statement.findByApp(appName)) {
      final ASTNode ast = statement.parsed();
      ast.context().setSchema(statement.app().schema("base", true));
      //      normalize(ast);
      inferred.putAll(InferForeignKey.analyze(statement.parsed()));
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
    for (Statement stmt : existing) keys.add(normalizeParam(stmt.parsed()).toString());

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
      final String key = normalizeParam(stmt.parsed()).toString();

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

  static void parameterize(ASTNode root) {
    normalizeParam(root);
  }

  static void normalize(ASTNode root) {
    clean(root);
    normalizeBool(root);
    normalizeTuple(root);
    normalizeConstantTable(root);
    normalizeJoinCondition(root);
  }

  static void retrofit(Statement stmt) {
    normalize(stmt.parsed());
    resolveBoolExpr(stmt.parsed());
    resolveParamFull(stmt.parsed());
  }

  private static int maxId(List<Statement> stmts) {
    return stmts.stream().mapToInt(Statement::stmtId).max().orElse(0);
  }
}
