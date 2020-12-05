package sjtu.ipads.wtune.solver.sql.expr;

import java.util.function.Consumer;

public interface ExprVisitor {
  boolean visit(Expr expr);

  static <T extends Expr> ExprVisitor seeker(Class<T> cls, Consumer<T> func) {
    return expr -> {
      if (cls.isInstance(expr)) func.accept((T) expr);
      return true;
    };
  }
}
