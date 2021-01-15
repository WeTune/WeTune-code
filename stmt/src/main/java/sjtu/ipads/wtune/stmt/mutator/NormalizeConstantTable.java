package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.collector.Collector;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SET_OP;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.JOINED;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

class NormalizeConstantTable {
  public static SQLNode normalize(SQLNode node) {
    Collector.collect(node, NormalizeConstantTable::canNormalize)
        .forEach(NormalizeConstantTable::normalizeConstantTable);
    return node;
  }

  private static boolean canNormalize(SQLNode node) {
    return DERIVED_SOURCE.isInstance(node) && JOINED.isInstance(node.parent()) && isConstantTable(node);
  }

  private static boolean isConstantTable(SQLNode derived) {
    final SQLNode subquery = derived.get(DERIVED_SUBQUERY);
    final SQLNode body = subquery.get(QUERY_BODY);
    return !SET_OP.isInstance(body)
        && body.get(QUERY_SPEC_FROM) == null
        && body.get(QUERY_SPEC_SELECT_ITEMS).stream()
            .map(SELECT_ITEM_EXPR::getFrom)
            .allMatch(LITERAL::isInstance);
  }

  private static void normalizeConstantTable(SQLNode table) {
    QueryScope scope = table.get(RESOLVED_QUERY_SCOPE);
    while (scope.parent() != null) scope = scope.parent();

    constantizeExprs(scope.queryNode(), table);
    reduceTable(table);
  }

  private static void constantizeExprs(SQLNode rootQuery, SQLNode tableSource) {
    for (SQLNode columnRef :
        Collector.collect(rootQuery, it -> it.get(RESOLVED_COLUMN_REF) != null)) {
      final ColumnRef rootRef = columnRef.get(RESOLVED_COLUMN_REF).resolveRootRef();

      if (rootRef == null || rootRef.source() == null) continue;
      if (tableSource != rootRef.source().node()) continue;

      // If the expr is an ORDER BY item then just remove it.
      // Consider "SELECT .. FROM (SELECT 1 AS o) t ORDER BY t.o"
      // "t.o" shouldn't be replaced as "1" because "ORDER BY 1"
      // means "order by the 1st output column".
      // It can be just removed since constant value won't affect
      // the ordering
      if (columnRef.get(RESOLVED_CLAUSE_SCOPE) == QueryScope.Clause.ORDER_BY) {
        final SQLNode parent = columnRef.parent();
        final SQLNode grandpa = parent.parent();
        if (QUERY.isInstance(grandpa)) {
          final List<SQLNode> orderItems = grandpa.get(QUERY_ORDER_BY);
          orderItems.remove(parent);
          if (orderItems.isEmpty()) grandpa.remove(QUERY_ORDER_BY);
          continue;
        }
      }

      final SQLNode replacement = rootRef.refItem().node().get(SELECT_ITEM_EXPR);

      // in-place substitute
      columnRef.remove(COLUMN_REF_COLUMN);
      columnRef.put(EXPR_KIND, LITERAL);
      columnRef.put(LITERAL_TYPE, replacement.get(LITERAL_TYPE));
      columnRef.put(LITERAL_VALUE, replacement.get(LITERAL_VALUE));
      columnRef.put(LITERAL_UNIT, replacement.get(LITERAL_UNIT));
    }
  }

  private static void reduceTable(SQLNode deriveTableSource) {
    final SQLNode joinNode = deriveTableSource.parent();
    final SQLNode left = joinNode.get(JOINED_LEFT);
    final SQLNode right = joinNode.get(JOINED_RIGHT);
    if (left == deriveTableSource) joinNode.update(right);
    else joinNode.update(left);
  }
}
