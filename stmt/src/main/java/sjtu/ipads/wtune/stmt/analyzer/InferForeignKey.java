package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.collector.Collector;
import sjtu.ipads.wtune.stmt.schema.Column;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.BINARY_RIGHT;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class InferForeignKey {
  public static Set<Column> analyze(SQLNode node) {
    final Set<Column> inferred = new HashSet<>();
    Collector.collect(node, InferForeignKey::isInteresting).forEach(it -> infer(it, inferred));
    return inferred;
  }

  private static boolean isInteresting(SQLNode node) {
    final BoolExpr boolExpr = node.get(BOOL_EXPR);
    return boolExpr != null && boolExpr.isJoinCondition();
  }

  private static void infer(SQLNode binary, Set<Column> inferred) {
    addSuspect(binary.get(BINARY_LEFT).get(RESOLVED_COLUMN_REF), inferred);
    addSuspect(binary.get(BINARY_RIGHT).get(RESOLVED_COLUMN_REF), inferred);
  }

  private static void addSuspect(ColumnRef cRef, Set<Column> inferred) {
    if (cRef != null) {
      final Column column = cRef.resolveAsColumn();
      if (column != null && !column.foreignKeyPart() && !column.uniquePart()) inferred.add(column);
    }
  }
}
