package sjtu.ipads.wtune.superopt.fragment1;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan1.Expr;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.all;

class ModelImpl implements Model {
  protected final ModelImpl base;
  protected final Map<Symbol, Object> assignments;

  ModelImpl(ModelImpl base) {
    this.base = base;
    this.assignments = new HashMap<>(8);
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
    if (outValues == null || all(outValues, it -> it.expr().isIdentity())) {
      return assign0(attrs, Pair.of(inValues, null));
    } else {
      return assign0(attrs, Pair.of(inValues, outValues));
    }
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
  public Pair<List<Value>, List<Value>> interpretAttrs(Symbol attrs) {
    return get0(attrs);
  }

  @Override
  public Model derive() {
    return new ModelImpl(this);
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
