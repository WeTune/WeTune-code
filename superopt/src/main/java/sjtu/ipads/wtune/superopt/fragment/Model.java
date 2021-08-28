package sjtu.ipads.wtune.superopt.fragment;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.Value;

import java.util.List;

public interface Model {
  boolean assign(Symbol table, PlanNode input);

  boolean assign(Symbol pred, Expr predicate);

  boolean assign(Symbol attrs, List<Value> values);

  boolean assign(Symbol attrs, List<Value> inValues, List<Value> outValues);

  boolean isInterpreted(Symbol sym);

  PlanNode interpretTable(Symbol table);

  Expr interpretPred(Symbol pred);

  List<Value> interpretInAttrs(Symbol attrs);

  Pair<List<Value>, List<Value>> interpretAttrs(Symbol attrs);

  Model base();

  Model derive();

  static Model mk() {
    return new ModelImpl(null);
  }
}
