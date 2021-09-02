package sjtu.ipads.wtune.sqlparser.plan;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.all;

class PlanContextImpl implements PlanContext {
  private final Schema schema;
  private final Map<Ref, Value> refTo = new IdentityHashMap<>();
  private final Map<Ref, PlanNode> refOwners = new IdentityHashMap<>();
  private final Map<Value, PlanNode> valueOwners = new IdentityHashMap<>();
  private BiMap<Value, Value> redirections;

  PlanContextImpl(Schema schema) {
    this.schema = schema;
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public Set<Ref> refs() {
    return refOwners.keySet();
  }

  @Override
  public Set<Value> values() {
    return valueOwners.keySet();
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
  public Value sourceOf(Value v) {
    if (!(v instanceof ExprValue) || !v.expr().isIdentity()) return v;
    else {
      return sourceOf(deRef(v.expr().refs().get(0)));
    }
  }

  @Override
  public void replaceValue(Value oldAttr, Value newAttr, Set<Ref> excludedRefs) {
    if (!valueOwners.containsKey(newAttr)) throw new NoSuchElementException();
    for (var pair : refTo.entrySet())
      if (pair.getValue() == oldAttr && !excludedRefs.contains(pair.getKey()))
        pair.setValue(newAttr);
  }

  @Override
  public void setRedirection(Value redirected, Value destination) {
    if (redirected == destination) return;
    if (redirections == null) redirections = HashBiMap.create();
    redirections.put(redirected, destination);
  }

  @Override
  public Value redirect(Value redirected) {
    return redirections == null ? redirected : redirections.getOrDefault(redirected, redirected);
  }

  @Override
  public void clearRedirections() {
    redirections = null;
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
