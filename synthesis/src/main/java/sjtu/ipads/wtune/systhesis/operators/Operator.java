package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_CLAUSE_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public interface Operator {
  default SQLNode apply(SQLNode sqlNode) {
    return apply(null, sqlNode);
  }

  default SQLNode apply(Statement stmt, SQLNode sqlNode) {
    return apply(sqlNode);
  }

  default SQLNode apply(Statement stmt) {
    return apply(stmt, stmt.parsed());
  }

  static void replaceNode(SQLNode target, SQLNode replacement) {
    final QueryScope scope = target.get(RESOLVED_QUERY_SCOPE);
    final QueryScope.Clause clause = target.get(RESOLVED_CLAUSE_SCOPE);

    final List<SQLNode> originalRefs = ColumnRefCollector.collect(target);
    final List<SQLNode> repRefs = ColumnRefCollector.collect(replacement);
    assert originalRefs.size() == repRefs.size();

    // replace column ref in replacement by original ones
    for (int i = 0; i < originalRefs.size(); i++) {
      final SQLNode originalRef = originalRefs.get(i);
      final SQLNode repRef = repRefs.get(i);

      repRef.replaceThis(originalRef);
    }

    // replace the target with replacement
    target.replaceThis(replacement);
    target.relinkAll();

    scope.setScope(target);
    clause.setClause(target);
  }
}
