package sjtu.ipads.wtune.sqlparser.plan;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.ExprImpl;

public interface Expr {
  List<Object> components();

  TIntList arity();

  List<ASTNode> columnRefs();

  static Expr combine(Expr expr0, Expr expr1) {
    final List<Object> components =
        new ArrayList<>(expr0.components().size() + expr1.components().size());
    components.addAll(expr0.components());
    components.addAll(expr1.components());

    final TIntArrayList arity = new TIntArrayList(expr0.arity().size() + expr1.arity().size());
    arity.addAll(expr0.arity());
    arity.addAll(expr1.arity());

    return make(components, arity);
  }

  static Expr make(Object obj) {
    return ExprImpl.build(obj);
  }

  static Expr make(List<Object> obj, TIntList arity) {
    return ExprImpl.build(obj, arity);
  }
}
