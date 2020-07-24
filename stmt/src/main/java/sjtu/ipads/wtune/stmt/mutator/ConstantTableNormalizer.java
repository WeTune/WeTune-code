package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.resolver.ColumnResolver;
import sjtu.ipads.wtune.stmt.resolver.Resolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;

/** Reduce 'constant table' like "FROM t INNER JOIN (SELECT 1 AS a) x WHERE t.a = x.a" */
public class ConstantTableNormalizer implements SQLVisitor, Mutator {
  private boolean modified = false;

  @Override
  public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    if (isConstantTable(derivedTableSource)
        && derivedTableSource.parent().get(TABLE_SOURCE_KIND) == SQLTableSource.Kind.JOINED) {

      QueryScope scope = derivedTableSource.get(RESOLVED_QUERY_SCOPE);
      while (scope.parent() != null) scope = scope.parent();

      inlineExpr(scope.queryNode(), derivedTableSource);
      reduceTable(derivedTableSource);

      derivedTableSource.parent().flagStructChanged(true);
      modified = true;
      return false;
    }
    return true;
  }

  private boolean isConstantTable(SQLNode derivedTableSource) {
    final SQLNode subquery = derivedTableSource.get(DERIVED_SUBQUERY);
    final SQLNode body = subquery.get(QUERY_BODY);
    if (body.type() == SQLNode.Type.SET_OP) return false;
    if (body.get(QUERY_SPEC_FROM) != null) return false;

    for (SQLNode item : body.get(QUERY_SPEC_SELECT_ITEMS))
      if (exprKind(item.get(SELECT_ITEM_EXPR)) != SQLExpr.Kind.LITERAL) return false;

    return true;
  }

  private void reduceTable(SQLNode deriveTableSource) {
    final SQLNode joinNode = deriveTableSource.parent();
    final SQLNode left = joinNode.get(JOINED_LEFT);
    final SQLNode right = joinNode.get(JOINED_RIGHT);
    if (left == deriveTableSource) joinNode.replaceThis(right);
    else joinNode.replaceThis(left);
  }

  private void inlineExpr(SQLNode rootQuery, SQLNode tableSource) {
    for (SQLNode columnRef : ColumnRefCollector.collect(rootQuery)) {
      final ColumnRef rootRef = columnRef.get(RESOLVED_COLUMN_REF).resolveRootRef();

      if (rootRef == null || rootRef.source() == null) continue;
      if (!nodeEquals(tableSource, rootRef.source().node())) continue;

      // If the expr is an ORDER BY item then just remove it.
      // Consider "SELECT .. FROM (SELECT 1 AS o) t ORDER BY t.o"
      // "t.o" shouldn't be replaced as "1" because "ORDER BY 1"
      // means "order by the 1st output column".
      // It can be just removed since constant value won't affect
      // the ordering
      if (columnRef.get(RESOLVED_CLAUSE_SCOPE) == QueryScope.Clause.ORDER_BY) {
        final SQLNode parent = columnRef.parent();
        final SQLNode grandpa = parent.parent();
        if (grandpa.type() == Type.QUERY) {
          final List<SQLNode> orderItems = grandpa.get(QUERY_ORDER_BY);
          orderItems.remove(parent);
          if (orderItems.isEmpty()) grandpa.remove(QUERY_ORDER_BY);
          return;
        }
      }

      final SQLNode replacement = rootRef.refItem().node().get(SELECT_ITEM_EXPR);

      // in-place substitute
      columnRef.remove(COLUMN_REF_COLUMN);
      columnRef.put(EXPR_KIND, SQLExpr.Kind.LITERAL);
      columnRef.put(LITERAL_TYPE, replacement.get(LITERAL_TYPE));
      columnRef.put(LITERAL_VALUE, replacement.get(LITERAL_VALUE));
      columnRef.put(LITERAL_UNIT, replacement.get(LITERAL_UNIT));
    }
  }

  @Override
  public void mutate(Statement stmt) {
    stmt.parsed().accept(this);
    if (modified) stmt.reResolve();
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(ColumnResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOnResolver() {
    return DEPENDENCIES;
  }
}
