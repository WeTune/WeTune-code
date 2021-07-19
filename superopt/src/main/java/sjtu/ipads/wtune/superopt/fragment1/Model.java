package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan1.Expr;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;

import java.util.List;

public interface Model {
  boolean assign(Symbol table, PlanNode input);

  boolean assign(Symbol pred, Expr predicate);

  boolean assign(Symbol attrs, List<Value> values);

  PlanNode interpretTable(Symbol table);

  Expr interpretPred(Symbol pred);

  List<Value> interpretAttrs(Symbol pred);
}
