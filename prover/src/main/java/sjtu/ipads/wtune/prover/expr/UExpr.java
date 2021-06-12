package sjtu.ipads.wtune.prover.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface UExpr {
  enum Kind {
    TABLE(0),
    ADD(2),
    MUL(2),
    NOT(1),
    SUM(1),
    SQUASH(1);

    public final int numChildren;

    Kind(int numChildren) {
      this.numChildren = numChildren;
    }
  }

  Kind kind();

  Set<Tuple> rootTuples();

  UExpr parent();

  UExpr child(int i);

  List<UExpr> children();

  // Note: parent are not allowed to change once being set.
  void setParent(UExpr parent);

  void setChild(int i, UExpr child);

  void replace(Tuple v1, Tuple v2);

  // Note: the copy's parent is not set.
  UExpr copy();

  static UExpr make(Kind kind) {
    switch (kind) {
      case ADD:
        return new AddExprImpl();
      case MUL:
        return new MulExprImpl();
      case NOT:
        return new NotExprImpl();
      case SQUASH:
        return new SquashExprImpl();
      case SUM:
        return new SumExprImpl();
      default:
        throw new IllegalArgumentException(kind + " expression need additional parameters");
    }
  }

  static UExpr mul(UExpr l, UExpr r) {
    final UExpr mul = new MulExprImpl();
    mul.setChild(0, l);
    mul.setChild(1, r);
    return mul;
  }

  static UExpr add(UExpr l, UExpr r) {
    final UExpr mul = new AddExprImpl();
    mul.setChild(0, l);
    mul.setChild(1, r);
    return mul;
  }

  static UExpr squash(UExpr x) {
    final UExpr expr = new SquashExprImpl();
    expr.setChild(0, x);
    return expr;
  }

  static UExpr sum(UExpr x) {
    final UExpr expr = new SumExprImpl();
    expr.setChild(0, x);
    return expr;
  }

  static UExpr not(UExpr x) {
    final UExpr expr = new NotExprImpl();
    expr.setChild(0, x);
    return expr;
  }

  static UExpr table(String tableName, Tuple tuple) {
    return new TableTermImpl(new NameImpl(tableName), tuple);
  }

  static UExpr otherSide(UExpr binaryExpr, UExpr self) {
    if (binaryExpr.kind().numChildren != 2)
      throw new IllegalArgumentException(binaryExpr + "is not a binary expr");

    final UExpr c0 = binaryExpr.child(0), c1 = binaryExpr.child(1);
    if (c0 == self) return c1;
    else if (c1 == self) return c0;
    else throw new IllegalArgumentException(self + " is not child of " + binaryExpr);
  }

  static UExpr rootOf(UExpr expr) {
    while (expr.parent() != null) expr = expr.parent();
    return expr;
  }

  static List<UExpr> suffixTraversal(UExpr root) {
    return suffixTraversal0(root, new ArrayList<>());
  }

  static void replaceChild(UExpr parent, UExpr child, UExpr rep) {
    final int numChildren = parent.kind().numChildren;
    boolean found = false;
    if (numChildren >= 1 && parent.child(0) == child) {
      parent.setChild(0, rep);
      found = true;
    }
    if (numChildren == 2 && parent.child(1) == child) {
      parent.setChild(1, rep);
      found = true;
    }

    if (!found) throw new IllegalStateException();
  }

  private static List<UExpr> suffixTraversal0(UExpr expr, List<UExpr> nodes) {
    for (UExpr child : expr.children()) suffixTraversal0(child, nodes);
    nodes.add(expr);
    return nodes;
  }
}
