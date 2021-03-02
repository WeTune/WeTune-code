package sjtu.ipads.wtune.sqlparser.util;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

public interface ASTHelper {
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
