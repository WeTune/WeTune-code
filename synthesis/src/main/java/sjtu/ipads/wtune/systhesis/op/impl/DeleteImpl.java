package sjtu.ipads.wtune.systhesis.op.impl;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;

public class DeleteImpl {
  public static void apply(SQLNode root, SQLNode target) {
    switch (target.type()) {
      case ORDER_ITEM:
        removeOrderItem(target);
        break;

      case GROUP_ITEM:
        removeGroupItem(target);
        break;

      case TABLE_SOURCE:
        removeTableSource(target);
        break;

      case QUERY:
        removeQuery(target);
        break;

      case EXPR:
        removeBoolExpr(target);
        break;

      default:
        assert false;
    }
  }

  private static void removeOrderItem(SQLNode target) {
    removeItem(target, QUERY_ORDER_BY);
  }

  private static void removeGroupItem(SQLNode target) {
    removeItem(target, QUERY_SPEC_GROUP_BY);
  }

  private static void removeItem(SQLNode target, Key<List<SQLNode>> key) {
    final SQLNode parent = target.parent();
    final List<SQLNode> items = parent.get(key);
    items.remove(target);
    if (items.isEmpty()) parent.remove(key);
  }

  private static void removeQuery(SQLNode target) {
    final SQLNode parent = target.parent();
    final SQLNode.Type parentType = parent.type();

    if (parentType == SQLNode.Type.SET_OP) {
      final SQLNode left = parent.get(SET_OP_LEFT);
      final SQLNode right = parent.get(SET_OP_RIGHT);
      removeBinaryOperand(parent, target, left, right);
      // QUERY_BODY should not be a QUERY
      // Thus extra care is needed after removal
      if (parent.type() == Type.QUERY && parent.parent().type() == Type.QUERY)
        parent.parent().replaceThis(parent);

    } else assert false;
  }

  private static void removeBoolExpr(SQLNode target) {
    final SQLNode parent = target.parent();
    final SQLNode.Type parentType = parent.type();

    if (parentType == Type.EXPR) {
      final SQLExpr.Kind parentKind = exprKind(parent);
      assert parentKind == SQLExpr.Kind.BINARY || parentKind == SQLExpr.Kind.UNARY;
      if (parentKind == SQLExpr.Kind.UNARY) {
        removeBoolExpr(parent);
        return;
      }

      final SQLNode left = parent.get(BINARY_LEFT);
      final SQLNode right = parent.get(BINARY_RIGHT);
      removeBinaryOperand(parent, target, left, right);

    } else if (parentType == Type.QUERY_SPEC) {
      if (parent.get(QUERY_SPEC_WHERE) == target) parent.remove(QUERY_SPEC_WHERE);
      else if (parent.get(QUERY_SPEC_HAVING) == target) parent.remove(QUERY_SPEC_HAVING);

    } else assert false;
  }

  private static void removeTableSource(SQLNode target) {
    final SQLNode parent = target.parent();
    final SQLNode.Type parentType = parent.type();

    if (parentType == Type.TABLE_SOURCE) {
      assert isJoined(parent);

      final SQLNode left = parent.get(JOINED_LEFT);
      final SQLNode right = parent.get(JOINED_RIGHT);
      removeBinaryOperand(parent, target, left, right);

    } else assert false;
  }

  private static void removeBinaryOperand(
      SQLNode parent, SQLNode target, SQLNode left, SQLNode right) {
    parent.replaceThis(target == left ? right : left);
  }
}
