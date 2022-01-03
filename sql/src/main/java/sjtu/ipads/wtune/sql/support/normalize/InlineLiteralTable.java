package sjtu.ipads.wtune.sql.support.normalize;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.resolution.Attribute;
import sjtu.ipads.wtune.sql.resolution.Relation;

import static sjtu.ipads.wtune.common.tree.TreeSupport.rootOf;
import static sjtu.ipads.wtune.common.utils.IterableSupport.all;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.ColRef;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.Literal;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.*;
import static sjtu.ipads.wtune.sql.ast1.TableSourceKind.DerivedSource;
import static sjtu.ipads.wtune.sql.ast1.TableSourceKind.JoinedSource;
import static sjtu.ipads.wtune.sql.resolution.ResolutionSupport.*;
import static sjtu.ipads.wtune.sql.support.NodeCollector.collect;

class InlineLiteralTable {
  static void normalize(SqlNode node) {
    final SqlNodes constantTables = collect(node, InlineLiteralTable::canNormalize);
    for (SqlNode constantTable : constantTables) normalizeConstantTable(constantTable);
  }

  private static boolean canNormalize(SqlNode node) {
    return DerivedSource.isInstance(node)
        && JoinedSource.isInstance(node.parent())
        && isConstantTable(node);
  }

  private static boolean isConstantTable(SqlNode derived) {
    final SqlNode subquery = derived.$(Derived_Subquery);
    final SqlNode body = subquery.$(Query_Body);
    return !SetOp.isInstance(body)
        && body.$(QuerySpec_From) == null
        && all(body.$(QuerySpec_SelectItems), it -> Literal.isInstance(it.$(SelectItem_Expr)));
  }

  private static void normalizeConstantTable(SqlNode table) {
    final SqlContext ctx = table.context();
    constantizeExprs(SqlNode.mk(ctx, rootOf(ctx, table.nodeId())), table);
    reduceTable(table);
  }

  private static void constantizeExprs(SqlNode rootQuery, SqlNode tableSource) {
    assert DerivedSource.isInstance(tableSource);
    final Relation targetRelation = getEnclosingRelation(tableSource.$(Derived_Subquery));
    final SqlNodes colRefs = collect(rootQuery, ColRef::isInstance);
    final SqlContext ctx = rootQuery.context();

    for (SqlNode colRef : colRefs) {
      final Attribute attr = resolveAttribute(colRef);
      if (attr == null) continue;

      final Attribute baseRef = traceRef(attr);
      if (baseRef == null || baseRef.owner() != targetRelation) continue;
      assert Literal.isInstance(baseRef.expr());

      // If the expr is an ORDER BY item then just remove it.
      // Consider "SELECT .. FROM (SELECT 1 AS o) t ORDER BY t.o"
      // "t.o" shouldn't be replaced as "1" because "ORDER BY 1"
      // means "order by the 1st output column".
      // It can be just removed since constant value won't affect
      // the ordering
      final SqlNode parent = colRef.parent();

      if (OrderItem.isInstance(parent)) {
        final SqlNode q = parent.parent();
        ctx.detachNode(parent.nodeId());
        if (Query.isInstance(q) && q.$(Query_OrderBy).isEmpty()) q.remove(Query_OrderBy);

      } else {
        final SqlNode copied = SqlSupport.copyAst(baseRef.expr()).go();
        ctx.displaceNode(colRef.nodeId(), copied.nodeId());
      }
    }
  }

  private static void reduceTable(SqlNode deriveTableSource) {
    final SqlContext ctx = deriveTableSource.context();
    final SqlNode body = getEnclosingRelation(deriveTableSource).rootNode().$(Query_Body);
    final SqlNode joinNode = deriveTableSource.parent();
    final SqlNode lhs = joinNode.$(Joined_Left);
    final SqlNode rhs = joinNode.$(Joined_Right);
    final SqlNode cond = joinNode.$(Joined_On);
    if (lhs == deriveTableSource) ctx.displaceNode(joinNode.nodeId(), rhs.nodeId());
    else ctx.displaceNode(joinNode.nodeId(), lhs.nodeId());
    assert QuerySpec.isInstance(body);
    NormalizationSupport.conjunctExprTo(body, QuerySpec_Where, cond);
  }
}
