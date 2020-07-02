package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class RemovePredicate implements Operator, SQLVisitor {
  private static final System.Logger LOG = System.getLogger("Synthesis.Operator.RemovePredicate");
  private final Long targetId;
  private boolean found = false;

  public RemovePredicate(Long targetId) {
    this.targetId = targetId;
  }

  public static Operator build(Long targetId) {
    return new RemovePredicate(targetId);
  }

  public static Operator build(SQLNode node) {
    if (node.type() != SQLNode.Type.EXPR) return null;
    if (node.get(BOOL_EXPR) == null) return null;
    return build(node.get(NODE_ID));
  }

  @Override
  public boolean enter(SQLNode node) {
    if (found) return false;

    final Long id = node.get(NODE_ID);
    final BoolExpr boolExpr = node.get(BOOL_EXPR);
    if (targetId.equals(id) && boolExpr == null) {
      LOG.log(
          System.Logger.Level.WARNING,
          "target found but not predicate: id={0} {1}",
          targetId,
          node);
      found = true;
      return false;
    }
    if (!targetId.equals(id)) return true;

    final SQLNode parent = node.parent();
    if (parent == null) {
      node.invalidate();
      return false;
    }

    if (parent.type() == SQLNode.Type.QUERY_SPEC && parent.get(QUERY_SPEC_WHERE) == node) {
      parent.put(QUERY_SPEC_WHERE, null);
      return false;
    }

    final SQLExpr.Kind parentKind = exprKind(parent);
    if (parentKind == SQLExpr.Kind.BINARY) {
      final SQLNode left = parent.get(BINARY_LEFT);
      final SQLNode right = parent.get(BINARY_RIGHT);
      if (left == node) parent.replaceThis(right);
      else if (right == node) parent.replaceThis(left);
      else assert false;

    } else {
      LOG.log(
          System.Logger.Level.WARNING,
          "target found but failed to remove. parent: {0} {1}",
          parentKind,
          parent);
    }

    return false;
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    if (targetId != null) sqlNode.accept(this);
    return sqlNode;
  }
}
