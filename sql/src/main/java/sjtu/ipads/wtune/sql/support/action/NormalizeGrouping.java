package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.support.resolution.Attribute;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sql.ast.ExprKind.ColRef;
import static sjtu.ipads.wtune.sql.ast.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.isConstant;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;
import static sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport.resolveAttribute;

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

  private static void convertFullCoveringGroupingToDistinct(SqlNode querySpec) {
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
}
