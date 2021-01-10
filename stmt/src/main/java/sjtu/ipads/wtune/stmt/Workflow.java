package sjtu.ipads.wtune.stmt;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.mutator.Mutation.*;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.*;

public interface Workflow {
  static Statement retrofit(Statement stmt) {
    clean(stmt.parsed());
    normalizeBool(stmt.parsed());
    normalizeTuple(stmt.parsed());
    resolveBoolExpr(stmt);
    resolveQueryScope(stmt);
    resolveTable(stmt);
    resolveColumnRef(stmt);
    resolveParamSimple(stmt);
    return stmt;
  }

  static void loadSQL(String appName, List<Statement> stmts) {
    final List<Statement> existing = Statement.findByApp(appName);

    final Set<String> keys = new HashSet<>();
    for (Statement stmt : existing) keys.add(retrofit(stmt).parsed().toString());

    int nextId = maxId(existing);
    for (Statement stmt : stmts) {
      final String key = retrofit(stmt).parsed().toString();
      if (!keys.contains(key)) {
        keys.add(key);
        stmt.setStmtId(++nextId);
        stmt.save();
      }
    }
  }

  private static int maxId(List<Statement> stmts) {
    return stmts.stream()
        .max(Comparator.comparing(Statement::appName))
        .map(Statement::stmtId)
        .orElse(0);
  }
}
