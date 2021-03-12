package sjtu.ipads.wtune.sqlparser.util;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;

public interface ASTHelper {
  static ASTNode makeSelectItem(ASTNode expr) {
    final ASTNode item = ASTNode.node(SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, expr);
    return item;
  }

  static String simpleName(String name) {
    return name == null ? null : unquoted(unquoted(name, '"'), '`').toLowerCase();
  }

  static String selectItemAlias(ASTNode selectItem) {
    final String alias = selectItem.get(SELECT_ITEM_ALIAS);
    if (alias != null) return alias;

    final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
    // Memo: Don't synthesize an alias for anonymous column.
    return COLUMN_REF.isInstance(expr) ? expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN) : null;
  }

  static boolean isForcedDistinct(ASTNode querySpec) {
    return querySpec.getOr(QUERY_SPEC_DISTINCT, false)
        || querySpec.get(QUERY_SPEC_DISTINCT_ON) != null;
  }

  static boolean isGlobalWildcard(List<ASTNode> selectItems) {
    if (selectItems.size() != 1) return false;
    final ASTNode expr = selectItems.get(0).get(SELECT_ITEM_EXPR);
    return WILDCARD.isInstance(expr) && expr.get(WILDCARD_TABLE) == null;
  }

  static ASTNode locateQueryNode(Relation relation) {
    final ASTNode node = relation.node();
    if (QUERY.isInstance(node)) return node;
    if (DERIVED_SOURCE.isInstance(node)) return node.get(DERIVED_SUBQUERY);
    else throw new IllegalArgumentException();
  }

  static ASTNode locateQuerySpecNode(Relation relation) {
    return locateQuerySpecNode0(relation.node());
  }

  private static ASTNode locateQuerySpecNode0(ASTNode node) {
    if (QUERY_SPEC.isInstance(node)) return node;
    if (QUERY.isInstance(node)) return locateQuerySpecNode0(node.get(QUERY_BODY));
    if (DERIVED_SOURCE.isInstance(node)) return locateQuerySpecNode0(node.get(DERIVED_SUBQUERY));
    if (SET_OP.isInstance(node)) return null;
    throw new IllegalArgumentException();
  }

  static boolean isAggFunc(String funcName) {
    return funcName != null && AGG_FUNCS.contains(simpleName(funcName));
  }

  Set<String> AGG_FUNCS =
      Set.of(
          "min",
          "max",
          "avg",
          "average",
          "sum",
          "count",
          "group_concat",
          "bit_and",
          "bit_or",
          "json_arrayagg",
          "json_objectagg",
          "std",
          "stddev",
          "stddev_pop",
          "stddev_samp",
          "var_pop",
          "var_samp",
          "variance");
}
