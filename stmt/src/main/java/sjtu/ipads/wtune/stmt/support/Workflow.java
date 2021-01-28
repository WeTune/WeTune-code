package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.dao.TimingDao;
import sjtu.ipads.wtune.stmt.rawlog.LogReader;
import sjtu.ipads.wtune.stmt.rawlog.RawStmt;
import sjtu.ipads.wtune.stmt.utils.FileUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.*;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveBoolExpr;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveParamFull;

public interface Workflow {
  static void loadTiming(String appName, String tag) {
    final Iterable<String> records = FileUtils.readLines("timing", appName + "." + tag + ".timing");
    Timing.fromLines(appName, tag, records).forEach(TimingDao.instance()::save);
  }

  static void loadSQL(String appName) {
    final Iterable<String> stmtLines = FileUtils.readLines("logs", appName, "stmts.log");
    final Iterable<String> traceLines = FileUtils.readLines("logs", appName, "traces.log");
    final List<RawStmt> rawStmts = LogReader.forTaggedFormat().readFrom(stmtLines, traceLines);
    final List<Statement> stmts =
        listMap(it -> Statement.build(appName, it.sql(), it.stackTrace().toString()), rawStmts);

    final List<Statement> existing = Statement.findByApp(appName);

    final Set<String> keys = new HashSet<>();
    for (Statement stmt : existing) keys.add(normalizeParam(stmt.parsed()).toString());

    int nextId = maxId(existing);
    for (Statement stmt : stmts) {
      final String key = normalizeParam(stmt.parsed()).toString();
      if (!keys.contains(key)) {
        keys.add(key);
        stmt.setStmtId(++nextId);
        StatementDao.instance().save(stmt);
      }
    }
  }

  static void retrofit(Statement stmt) {
    clean(stmt.parsed());
    normalizeBool(stmt.parsed());
    normalizeTuple(stmt.parsed());
    normalizeConstantTable(stmt.parsed());
    resolveBoolExpr(stmt.parsed());
    resolveParamFull(stmt.parsed());
  }

  private static int maxId(List<Statement> stmts) {
    return stmts.stream()
        .max(Comparator.comparing(Statement::appName))
        .map(Statement::stmtId)
        .orElse(0);
  }
}
