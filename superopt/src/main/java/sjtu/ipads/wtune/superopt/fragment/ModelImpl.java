package sjtu.ipads.wtune.superopt.fragment;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

class ModelImpl implements Model {
  protected final ModelImpl base;
  protected final Map<Symbol, Object> assignments;

  ModelImpl(ModelImpl base) {
    this.base = base;
    this.assignments = new HashMap<>(16);
  }

  @Override
  public boolean assign(Symbol table, PlanNode input) {
    return assign0(table, input);
  }

  @Override
  public boolean assign(Symbol pred, Expr predicate) {
    return assign0(pred, predicate);
  }

  @Override
  public boolean assign(Symbol attrs, List<Value> values) {
    return assign(attrs, values, null);
  }

  @Override
  public boolean assign(Symbol attrs, List<Value> inValues, List<Value> outValues) {
    return assign0(attrs, Pair.of(inValues, outValues));
  }

  @Override
  public boolean isInterpreted(Symbol sym) {
    return get0(sym) != null;
  }

  @Override
  public PlanNode interpretTable(Symbol table) {
    return get0(table);
  }

  @Override
  public Expr interpretPred(Symbol pred) {
    return get0(pred);
  }

  @Override
  public List<Value> interpretInAttrs(Symbol attrs) {
    final Pair<List<Value>, List<Value>> pair = get0(attrs);
    return pair == null ? null : pair.getLeft();
  }

  @Override
  public List<List<Value>> interpretOutAttrs(Symbol attrs) {
    final Pair<List<Value>, List<Value>> pair = get0(attrs);
    return pair == null ? null : singletonList(pair.getRight());
  }

  @Override
  public Model base() {
    return base;
  }

  @Override
  public Model derive() {
    return new ModelImpl(this);
  }

  @Override
  public PlanContext planContext() {
    return null;
  }

  @Override
  public boolean isAssignmentTrusted(Symbol sym) {
    return true;
  }

  protected boolean assign0(Symbol sym, Object obj) {
    assignments.put(sym, obj);
    return true;
  }

  protected <T> T get0(Symbol key) {
    final Object obj = assignments.get(key);
    if (obj != null) return (T) obj;
    else if (base != null) return base.get0(key);
    else return null;
  }
}
