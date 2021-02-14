package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.stmt.utils.Collector;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;

class NormalizeConstantTable {
  public static ASTNode normalize(ASTNode node) {
    Collector.collect(node, NormalizeConstantTable::canNormalize)
        .forEach(NormalizeConstantTable::normalizeConstantTable);
    return node;
  }

  private static boolean canNormalize(ASTNode node) {
    return DERIVED_SOURCE.isInstance(node)
           && JOINED_SOURCE.isInstance(node.parent())
           && isConstantTable(node);
  }

  private static boolean isConstantTable(ASTNode derived) {
    final ASTNode subquery = derived.get(DERIVED_SUBQUERY);
    final ASTNode body = subquery.get(QUERY_BODY);
    return !SET_OP.isInstance(body)
        && body.get(QUERY_SPEC_FROM) == null
        && body.get(QUERY_SPEC_SELECT_ITEMS).stream()
            .map(SELECT_ITEM_EXPR::get)
            .allMatch(LITERAL::isInstance);
  }

  private static void normalizeConstantTable(ASTNode table) {
    Relation relation = table.get(RELATION);
    while (relation.parent() != null) relation = relation.parent();

    constantizeExprs(relation.node(), table);
    reduceTable(table);
  }

  private static void constantizeExprs(ASTNode rootQuery, ASTNode tableSource) {
    final Relation targetRelation = tableSource.get(RELATION);

    for (ASTNode columnRef : Collector.collect(rootQuery, COLUMN_REF::isInstance)) {
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
      final ASTNode parent = columnRef.parent();
      if (ORDER_ITEM.isInstance(parent)) {
        final ASTNode grandpa = parent.parent();
        if (QUERY.isInstance(grandpa)) {
          final List<ASTNode> orderItems = grandpa.get(QUERY_ORDER_BY);
          orderItems.remove(parent);

          if (orderItems.isEmpty()) grandpa.unset(QUERY_ORDER_BY);
          continue;
        }
      }

      // in-place substitute
      final ASTNode replacement = rootRef.node().get(SELECT_ITEM_EXPR);
      columnRef.unset(COLUMN_REF_COLUMN);
      columnRef.update(replacement);
    }
  }

  private static void reduceTable(ASTNode deriveTableSource) {
    final ASTNode joinNode = deriveTableSource.parent();
    final ASTNode left = joinNode.get(JOINED_LEFT);
    final ASTNode right = joinNode.get(JOINED_RIGHT);
    if (left == deriveTableSource) joinNode.update(right);
    else joinNode.update(left);
  }
}
