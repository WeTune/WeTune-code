package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;
import sjtu.ipads.wtune.stmt.utils.Collector;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.JOINED;
import static sjtu.ipads.wtune.sqlparser.rel.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

class NormalizeConstantTable {
  public static SQLNode normalize(SQLNode node) {
    Collector.collect(node, NormalizeConstantTable::canNormalize)
        .forEach(NormalizeConstantTable::normalizeConstantTable);
    return node;
  }

  private static boolean canNormalize(SQLNode node) {
    return DERIVED_SOURCE.isInstance(node)
        && JOINED.isInstance(node.parent())
        && isConstantTable(node);
  }

  private static boolean isConstantTable(SQLNode derived) {
    final SQLNode subquery = derived.get(DERIVED_SUBQUERY);
    final SQLNode body = subquery.get(QUERY_BODY);
    return !SET_OP.isInstance(body)
        && body.get(QUERY_SPEC_FROM) == null
        && body.get(QUERY_SPEC_SELECT_ITEMS).stream()
            .map(SELECT_ITEM_EXPR::get)
            .allMatch(LITERAL::isInstance);
  }

  private static void normalizeConstantTable(SQLNode table) {
    Relation relation = table.get(RELATION);
    while (relation.parent() != null) relation = relation.parent();

    constantizeExprs(relation.node(), table);
    reduceTable(table);
  }

  private static void constantizeExprs(SQLNode rootQuery, SQLNode tableSource) {
    final Relation targetRelation = tableSource.get(RELATION);

    for (SQLNode columnRef : Collector.collect(rootQuery, COLUMN_REF::isInstance)) {
      final Attribute attr = columnRef.get(ATTRIBUTE);
      if (attr == null) continue;

      final Attribute rootRef = attr.reference(true);

      if (rootRef.owner() != targetRelation) continue;
      assert LITERAL.isInstance(rootRef.node().get(SELECT_ITEM_EXPR));

      // If the expr is an ORDER BY item then just remove it.
      // Consider "SELECT .. FROM (SELECT 1 AS o) t ORDER BY t.o"
      // "t.o" shouldn't be replaced as "1" because "ORDER BY 1"
      // means "order by the 1st output column".
      // It can be just removed since constant value won't affect
      // the ordering
      final SQLNode parent = columnRef.parent();
      if (ORDER_ITEM.isInstance(parent)) {
        final SQLNode grandpa = parent.parent();
        if (QUERY.isInstance(grandpa)) {
          final List<SQLNode> orderItems = grandpa.get(QUERY_ORDER_BY);
          orderItems.remove(parent);

          if (orderItems.isEmpty()) grandpa.unset(QUERY_ORDER_BY);
          continue;
        }
      }

      // in-place substitute
      final SQLNode replacement = rootRef.node().get(SELECT_ITEM_EXPR);
      columnRef.unset(COLUMN_REF_COLUMN);
      columnRef.update(replacement);
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
