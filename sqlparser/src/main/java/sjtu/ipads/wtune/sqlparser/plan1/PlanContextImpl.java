package sjtu.ipads.wtune.sqlparser.plan1;

import static sjtu.ipads.wtune.common.utils.FuncUtils.all;

import java.util.IdentityHashMap;
import java.util.Map;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

class PlanContextImpl implements PlanContext {
  private final Schema schema;
  private final Map<Ref, Value> refTo = new IdentityHashMap<>();
  private final Map<Ref, PlanNode> refOwners = new IdentityHashMap<>();
  private final Map<Value, PlanNode> valueOwners = new IdentityHashMap<>();

  PlanContextImpl(Schema schema) {
    this.schema = schema;
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public Value deRef(Ref ref) {
    return refTo.get(ref);
  }

  @Override
  public void registerValues(PlanNode node, ValueBag values) {
    values.forEach(it -> valueOwners.put(it, node));
  }

  @Override
  public void registerRefs(PlanNode node, RefBag refs) {
    refs.forEach(it -> refOwners.put(it, node));
  }

  @Override
  public PlanNode ownerOf(Value value) {
    return valueOwners.get(value);
  }

  @Override
  public PlanNode ownerOf(Ref ref) {
    return refOwners.get(ref);
  }

  @Override
  public void setRef(Ref ref, Value value) {
    refTo.put(ref, value);
  }

  @Override
  public boolean isSameSource(Value v0, Value v1) {
    if (v0 == v1) return true;
    if (v0 == null || v1 == null) return false;
    final Value src0 = sourceOf(v0), src1 = sourceOf(v1);
    return src0.qualification().equals(src1.qualification()) && src0.name().equals(src1.name());
  }

  @Override
  public boolean validate() {
    return all(refOwners.keySet(), it -> deRef(it) != null);
  }

  private Value sourceOf(Value v) {
    if (!(v instanceof ExprValue) || !v.expr().isIdentity()) return v;
    else return sourceOf(deRef(v.expr().refs().get(0)));
  }
}
