package sjtu.ipads.wtune.prover.expr;

import static sjtu.ipads.wtune.common.utils.FuncUtils.tautology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public interface UExpr {
  enum Kind {
    TABLE(0),
    PRED(0),
    EQ_PRED(0),
    ADD(2),
    MUL(2),
    NOT(1),
    SUM(1),
    SQUASH(1);

    public final int numChildren;

    Kind(int numChildren) {
      this.numChildren = numChildren;
    }

    public boolean isPred() {
      return this == PRED || this == EQ_PRED;
    }

    public boolean isTerm() {
      return this.numChildren == 0;
    }
  }

  Kind kind();

  UExpr parent();

  UExpr child(int i);

  List<UExpr> children();

  // Note: parent are not allowed to change once being set.
  void setParent(UExpr parent);

  void setChild(int i, UExpr child);

  void subst(Tuple v1, Tuple v2);

  // Note: the copy's parent is not set.
  UExpr copy();

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

  static UExpr sum(Tuple boundTuple, UExpr x) {
    final UExpr expr = new SumExprImpl(Collections.singletonList(boundTuple));
    expr.setChild(0, x);
    return expr;
  }

  static UExpr sum(List<Tuple> boundTuple, UExpr x) {
    final UExpr expr = new SumExprImpl(boundTuple);
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

  static UExpr uninterpretedPred(String predName, Tuple... tuple) {
    return new UninterpretedPredTermImpl(new NameImpl(predName), tuple);
  }

  static UExpr eqPred(Tuple left, Tuple right) {
    return new EqPredTermImpl(left, right);
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
    return suffixTraversal0(root, tautology(), new ArrayList<>());
  }

  static List<UExpr> suffixTraversal(UExpr root, Kind kind) {
    return suffixTraversal0(root, it -> it.kind() == kind, new ArrayList<>());
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

  static UExpr validate(UExpr expr) {
    for (UExpr child : expr.children()) {
      if (child.parent() != expr) {
        return expr;
      }
      final UExpr invalid = validate(child);
      if (invalid != null) return invalid;
    }
    return null;
  }

  private static List<UExpr> suffixTraversal0(
      UExpr expr, Predicate<UExpr> filter, List<UExpr> nodes) {
    for (UExpr child : expr.children()) suffixTraversal0(child, filter, nodes);
    if (filter.test(expr)) nodes.add(expr);
    return nodes;
  }
}
