package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.resovler.JoinConditionResolver;
import sjtu.ipads.wtune.stmt.resovler.Resolver;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.BINARY_RIGHT;
import static sjtu.ipads.wtune.stmt.attrs.AppAttrs.IMPLIED_FOREIGN_KEYS;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class ImpliedForeignKeyAnalyzer implements Analyzer<Void>, SQLVisitor {
  private Set<Column> impliedForeignKey;

  @Override
  public Void analyze(Statement stmt) {
    impliedForeignKey = stmt.appContext().supplyIfAbsent(IMPLIED_FOREIGN_KEYS, HashSet::new);
    stmt.parsed().accept(this);
    return null;
  }

  @Override
  public boolean enterBinary(SQLNode binary) {
    final BoolExpr boolExpr = binary.get(BOOL_EXPR);
    if (boolExpr == null || !boolExpr.isJoinCondtion()) return true;

    final ColumnRef leftRef = binary.get(BINARY_LEFT).get(RESOLVED_COLUMN_REF);
    final ColumnRef rightRef = binary.get(BINARY_RIGHT).get(RESOLVED_COLUMN_REF);

    addSuspect(leftRef);
    addSuspect(rightRef);

    return false;
  }

  private void addSuspect(ColumnRef cRef) {
    if (cRef == null) return;
    final Column column = cRef.resolveAsColumn();
    if (column == null || column.foreignKeyPart() || column.uniquePart()) return;
    impliedForeignKey.add(column);
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(JoinConditionResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
