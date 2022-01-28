package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.support.resolution.Attribute;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sql.ast.ExprKind.Aggregate;
import static sjtu.ipads.wtune.sql.ast.ExprKind.ColRef;
import static sjtu.ipads.wtune.sql.ast.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.*;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.predicateLocator;
import static sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport.getEnclosingRelation;
import static sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport.resolveAttribute;
import static sjtu.ipads.wtune.sql.util.RenumberListener.watch;

class NormalizeGrouping {
  static void normalize(SqlNode node) {
    for (SqlNode target : nodeLocator().accept(QuerySpec).gather(node)) normalizeGrouping(target);
  }

  private static void normalizeGrouping(SqlNode querySpec) {
    final SqlNodes groupBys = querySpec.$(QuerySpec_GroupBy);
    if (Commons.isNullOrEmpty(groupBys)) return;

    removeConstantGroupItem(groupBys);
    if (groupBys.isEmpty()) {
      querySpec.remove(QuerySpec_GroupBy);
      return;
    }

    sortGroupItem(groupBys);
    convertHavingToWhere(querySpec);
    convertFullCoveringGroupingToDistinct(querySpec);
  }

  private static void removeConstantGroupItem(SqlNodes groupBys) {
    for (int i = 0; i < groupBys.size(); ) {
      final SqlNode groupItem = groupBys.get(i);
      if (isConstant(groupItem.$(GroupItem_Expr))) {
        groupBys.erase(groupItem.nodeId());
      } else {
        ++i;
      }
    }
  }

  private static void sortGroupItem(SqlNodes groupBys) {
    groupBys.sort(Comparator.comparing(SqlNode::toString));
  }

  private static void convertFullCoveringGroupingToDistinct(SqlNode querySpec) {
    if (querySpec.$(QuerySpec_Having) != null) return;
    final SqlNodes groupBys = querySpec.$(QuerySpec_GroupBy);

    final Set<Attribute> groupAttributes = new HashSet<>();
    for (SqlNode groupBy : groupBys) {
      final SqlNode expr = groupBy.$(GroupItem_Expr);
      if (!ColRef.isInstance(expr)) return;

      final Attribute attribute = resolveAttribute(expr);
      if (attribute == null) return;

      groupAttributes.add(attribute);
    }

    final SqlNodes projections = querySpec.$(QuerySpec_SelectItems);
    for (SqlNode projection : projections) {
      final SqlNode expr = projection.$(SelectItem_Expr);
      if (!ColRef.isInstance(expr)) return;

      final Attribute attribute = resolveAttribute(expr);
      if (attribute == null) return;

      if (!groupAttributes.contains(attribute)) return;
    }

    querySpec.remove(QuerySpec_GroupBy);
    querySpec.flag(QuerySpec_Distinct);
  }

  private static void convertHavingToWhere(SqlNode querySpec) {
    final SqlNode having = querySpec.$(QuerySpec_Having);
    if (having == null) return;

    final SqlNodes exprs =
        predicateLocator().primitive().conjunctive().breakdownExpr().gather(having);

    try (final var es = watch(querySpec.context(), exprs.nodeIds())) {
      for (SqlNode e : es) {
        convertHavingToWhere(querySpec, e);
      }
    }
  }

  private static void convertHavingToWhere(SqlNode querySpec, SqlNode expr) {
    final SqlNode agg = nodeLocator().accept(Aggregate).find(expr);
    if (agg != null) return;

    final SqlNodes colRefs = nodeLocator().accept(ColRef).gather(expr);
    final List<Attribute> outAttr = getEnclosingRelation(querySpec).attributes();
    for (SqlNode colRef : colRefs) {
      final Attribute attr = resolveAttribute(colRef);
      if (attr != null && outAttr.contains(attr)) return;
    }

    detachExpr(expr);
    conjunctExprTo(querySpec, QuerySpec_Where, expr);
  }
}
