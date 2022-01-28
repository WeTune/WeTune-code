package sjtu.ipads.wtune.sql.support.locator;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlVisitor;
import sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast.SqlKind.Query;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.QuerySpec_Having;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.QuerySpec_Where;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Joined_On;

class PredicateLocator implements SqlVisitor, SqlGatherer, SqlFinder {
  private final TIntList nodes;
  private final boolean scoped;
  private final boolean bottomUp;
  private final boolean primitive;
  private final boolean conjunctionOnly;
  private final boolean breakdownExpr;
  private int exemptQueryNode;

  protected PredicateLocator(
      boolean scoped,
      boolean bottomUp,
      boolean primitive,
      boolean conjunctionOnly,
      boolean breakdownExpr,
      int expectedNumNodes) {
    this.nodes = expectedNumNodes >= 0 ? new TIntArrayList(expectedNumNodes) : new TIntArrayList();
    this.bottomUp = bottomUp;
    this.scoped = scoped;
    this.primitive = primitive;
    this.breakdownExpr = breakdownExpr;
    this.conjunctionOnly = conjunctionOnly;
  }

  @Override
  public int find(SqlNode root) {
    if (breakdownExpr && Expr.isInstance(root)) {
      traversePredicate(root);
    } else {
      exemptQueryNode = Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
      root.accept(this);
    }
    return nodes.isEmpty() ? NO_SUCH_NODE : nodes.get(0);
  }

  @Override
  public TIntList gather(SqlNode root) {
    if (breakdownExpr && Expr.isInstance(root)) {
      traversePredicate(root);
    } else {
      exemptQueryNode = Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
      root.accept(this);
    }

    return nodes;
  }

  @Override
  public TIntList nodeIds() {
    return nodes;
  }

  @Override
  public boolean enterQuery(SqlNode query) {
    return !scoped || query.nodeId() == exemptQueryNode;
  }

  @Override
  public boolean enterCase(SqlNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.$(Case_Cond) == null;
  }

  @Override
  public boolean enterWhen(SqlNode when) {
    if (!bottomUp && when != null) traversePredicate(when.$(When_Cond));
    return false;
  }

  @Override
  public boolean enterChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (!bottomUp
        && child != null
        && (key == Joined_On || key == QuerySpec_Where || key == QuerySpec_Having)) {
      traversePredicate(child);
    }
    return true;
  }

  @Override
  public void leaveChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (bottomUp
        && child != null
        && (key == Joined_On || key == QuerySpec_Where || key == QuerySpec_Having)) {
      traversePredicate(child);
    }
  }

  @Override
  public void leaveWhen(SqlNode when) {
    if (bottomUp && when != null) nodes.add(when.$(When_Cond).nodeId());
  }

  private void traversePredicate(SqlNode expr) {
    assert Expr.isInstance(expr);
    // `expr` must be evaluated as boolean

    if (Binary.isInstance(expr) && expr.$(Binary_Op).isLogic()) {
      if (conjunctionOnly) {
        final BinaryOpKind op = expr.$(Binary_Op);
        if (op == BinaryOpKind.AND) {
          if (!primitive && !bottomUp) nodes.add(expr.nodeId());
          traversePredicate(expr.$(Binary_Left));
          traversePredicate(expr.$(Binary_Right));
          if (!primitive && bottomUp) nodes.add(expr.nodeId());
        } else {
          nodes.add(expr.nodeId());
        }
      } else {
        if (!primitive && !bottomUp) nodes.add(expr.nodeId());
        traversePredicate(expr.$(Binary_Left));
        traversePredicate(expr.$(Binary_Right));
        if (!primitive && bottomUp) nodes.add(expr.nodeId());
      }

    } else if (Unary.isInstance(expr) && expr.$(Unary_Op).isLogic()) {
      if (!primitive && !bottomUp) nodes.add(expr.nodeId());
      traversePredicate(expr.$(Unary_Expr));
      if (!primitive && bottomUp) nodes.add(expr.nodeId());

    } else {
      nodes.add(expr.nodeId());
    }
  }
}
