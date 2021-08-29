package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
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

  List<List<Value>> interpretOutAttrs(Symbol attrs);

  boolean isAssignmentTrusted(Symbol sym);

  Model base();

  Model derive();

  PlanContext planContext();

  static Model mk() {
    return new ModelImpl(null);
  }
}
