package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.IdentityHashMap;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.all;

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
  public boolean validate() {
    return all(refOwners.keySet(), it -> deRef(it) != null);
  }
}
