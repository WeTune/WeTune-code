package sjtu.ipads.wtune.sql.util;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.ASTVistor;
import sjtu.ipads.wtune.sql.ast.FieldDomain;
import sjtu.ipads.wtune.sql.relational.Relation;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.NodeFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sql.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sql.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sql.relational.Attribute.ATTRIBUTE;

public interface ASTHelper {
  class ColumnRefChecker implements ASTVistor {
    ASTNode invalidNode = null;

    @Override
    public boolean enter(ASTNode node) {
      return invalidNode == null;
    }

    @Override
    public boolean enterColumnRef(ASTNode columnRef) {
      if (columnRef.get(ATTRIBUTE) == null) {
        invalidNode = columnRef;
        return false;
      }
      return true;
    }
  }

  class ConformityChecker implements ASTVistor {
    Pair<ASTNode, ASTNode> invalidPair = null;

    @Override
    public boolean enter(ASTNode node) {
      return invalidPair == null;
    }

    @Override
    public boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
      if (child != null && child.parent() != parent) {
        invalidPair = Pair.of(parent, child);
        return false;
      }
      return true;
    }

    @Override
    public boolean enterChildren(
        ASTNode parent, FieldKey<List<ASTNode>> key, List<ASTNode> children) {
      if (children != null)
        for (ASTNode child : children)
          if (child.parent() != parent) {
            invalidPair = Pair.of(parent, child);
            return false;
          }

      return true;
    }
  }

  static ASTNode findInvalidColumnRefs(ASTNode node) {
    final ColumnRefChecker checker = new ColumnRefChecker();
    node.accept(checker);
    return checker.invalidNode;
  }

  static Pair<ASTNode, ASTNode> findDanglingNode(ASTNode node) {
    final ConformityChecker checker = new ConformityChecker();
    node.accept(checker);
    return checker.invalidPair;
  }

  static String quoted(String dbType, String name) {
    if ("mysql".equals(dbType)) return '`' + name + '`';
    else if ("postgresql".equals(dbType)) return '"' + name + '"';
    else if ("mssql".equals(dbType)) return '[' + name + ']';
    else throw new IllegalArgumentException("unknown db type: " + dbType);
  }

  static String simpleName(String name) {
    return name == null ? null : unquoted(unquoted(name, '"'), '`').toLowerCase();
  }

  static ASTNode makeSelectItem(ASTNode expr) {
    final ASTNode item = ASTNode.node(SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, expr);
    return item;
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

  static ASTNode locateOtherSide(ASTNode binaryExpr, ASTNode thisSide) {
    if (!BINARY.isInstance(binaryExpr)) return null;
    final ASTNode left = binaryExpr.get(BINARY_LEFT);
    final ASTNode right = binaryExpr.get(BINARY_RIGHT);
    if (left == thisSide) return right;
    if (right == thisSide) return left;
    return null;
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

  static boolean isEnclosedBy(ASTNode node, FieldDomain type) {
    while (node.parent() != null) {
      if (type.isInstance(node)) return true;
      node = node.parent();
    }
    return false;
  }

  static boolean isParentedBy(ASTNode node, FieldKey key) {
    final ASTNode parent = node.parent();
    if (parent == null) return false;
    if (parent.get(key) == node) return true;
    else return isParentedBy(parent, key);
  }

  static boolean isAggSelection(ASTNode selectItem) {
    return AGGREGATE.isInstance(selectItem.get(SELECT_ITEM_EXPR));
  }

  static boolean isAggSelectionWithDistinct(ASTNode selectItem) {
    final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
    return AGGREGATE.isInstance(expr) && expr.isFlag(AGGREGATE_DISTINCT);
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
