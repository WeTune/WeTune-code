package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.IdentityHashMap;
import java.util.Map;

class PlanContextImpl implements PlanContext {
  private final Map<Ref, Value> refTo = new IdentityHashMap<>();
  private final Map<Ref, PlanNode> refOwners = new IdentityHashMap<>();
  private final Map<Value, PlanNode> valueOwners = new IdentityHashMap<>();

  PlanContextImpl() {}

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
}
