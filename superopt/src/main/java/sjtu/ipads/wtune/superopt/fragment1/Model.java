package sjtu.ipads.wtune.superopt.fragment1;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan1.Expr;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;

public interface Model {
  boolean assign(Symbol table, PlanNode input);

  boolean assign(Symbol pred, Expr predicate);

  boolean assign(Symbol attrs, List<Value> values);

  PlanNode interpretTable(Symbol table);

  Expr interpretPred(Symbol pred);

  List<Value> interpretAttrs(Symbol pred);

  Model derive();

  static Model mk() {
    return new ModelImpl(null);
  }
}
