package sjtu.ipads.wtune.sql.support.normalize;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.resolution.Attribute;
import sjtu.ipads.wtune.sql.resolution.Relation;
import sjtu.ipads.wtune.sql.support.ClauseCollector;
import sjtu.ipads.wtune.sql.support.NodeCollector;

import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.sql.SqlSupport.*;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.QuerySpec_Where;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.Joined_Kind;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.Joined_On;
import static sjtu.ipads.wtune.sql.ast1.constants.BinaryOpKind.AND;
import static sjtu.ipads.wtune.sql.ast1.constants.UnaryOpKind.NOT;
import static sjtu.ipads.wtune.sql.resolution.ResolutionSupport.*;
import static sjtu.ipads.wtune.sql.support.RenumberListener.watch;
import static sjtu.ipads.wtune.sql.support.normalize.NormalizationSupport.conjunctExprTo;
import static sjtu.ipads.wtune.sql.support.normalize.NormalizationSupport.detachExpr;

class NormalizeJoinCond {
  static void normalize(SqlNode root) {
    NodeCollector.collect(root, QuerySpec::isInstance).forEach(NormalizeJoinCond::process);
  }

  private static void process(SqlNode querySpec) {
    final TIntList plainConds = collectPlainCondition(querySpec);
    try (final var exprs = watch(querySpec.context(), plainConds)) {
      for (SqlNode expr : exprs) {
        detachExpr(expr);
        conjunctExprTo(querySpec, QuerySpec_Where, expr);
      }
    }

    final TIntList joinConds = collectJoinCondition(querySpec);
    try (final var exprs = watch(querySpec.context(), joinConds)) {
      for (SqlNode expr : exprs) {
        final SqlNode targetJoin = locateJoinSource(expr);
        if (targetJoin != null) {
          detachExpr(expr);
          conjunctExprTo(targetJoin, Joined_On, expr);
        }
      }
    }
  }

  private static TIntList collectPlainCondition(SqlNode querySpec) {
    final SqlNodes onConditions = ClauseCollector.collect(querySpec, Joined_On);
    if (onConditions.isEmpty()) return new TIntArrayList(0);

    final TIntList plainConditions = new TIntArrayList(onConditions.size());
    for (SqlNode onCond : onConditions) {
      if (!onCond.parent().$(Joined_Kind).isInner()) continue;

      final List<SqlNode> terms = linearizeConjunction(onCond);
      for (SqlNode term : terms) if (isPlainCondition(term)) plainConditions.add(term.nodeId());
    }

    return plainConditions;
  }

  private static TIntList collectJoinCondition(SqlNode querySpec) {
    final SqlNode whereClause = querySpec.$(QuerySpec_Where);
    if (whereClause == null) return new TIntArrayList(0);

    final NodeCollector collector =
        new NodeCollector() {
          @Override
          protected int check(SqlNode node) {
            if (!Expr.isInstance(node)) return STOP;
            if (Binary.isInstance(node)) {
              if (AND == node.$(Binary_Op)) return NOT_ACCEPT;
              else if (isColRefEq(node)) return ACCEPT | STOP;
            }
            return STOP;
          }
        };

    whereClause.accept(collector);
    return collector.nodeIds();
  }

  private static boolean isPlainCondition(SqlNode expr) {
    if (Unary.isInstance(expr))
      return expr.$(Unary_Op) == NOT && isPlainCondition(expr.$(Unary_Expr));

    if (Binary.isInstance(expr))
      return !expr.$(Binary_Op).isLogic()
          && (!ColRef.isInstance(expr.$(Binary_Left)) || !ColRef.isInstance(expr.$(Binary_Right)));

    return false;
  }

  private static SqlNode locateJoinSource(SqlNode joinCond) {
    assert isColRefEq(joinCond);

    final Attribute lhs = resolveAttribute(joinCond.$(Binary_Left));
    final Attribute rhs = resolveAttribute(joinCond.$(Binary_Right));
    final List<Relation> inputs = getEnclosingRelation(joinCond).inputs();
    for (int i = 1, bound = inputs.size(); i < bound; ++i) {
      final List<Relation> visibleInputs = inputs.subList(0, i + 1);
      if (isAttributePresent(lhs, visibleInputs) && isAttributePresent(rhs, visibleInputs)) {
        return tableSourceOf(inputs.get(i)).parent();
      }
    }

    return null;
  }

  private static boolean isAttributePresent(Attribute attr, List<Relation> relations) {
    for (Relation relation : relations) if (relation.attributes().contains(attr)) return true;
    return false;
  }
}
