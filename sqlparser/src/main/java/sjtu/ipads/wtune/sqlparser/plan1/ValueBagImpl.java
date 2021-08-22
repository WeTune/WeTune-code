package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.head;

class ValueBagImpl extends AbstractList<Value> implements ValueBag {
  private final List<Value> values;

  static final ValueBag EMPTY = new ValueBagImpl(Collections.emptyList());

  ValueBagImpl(List<Value> values) {
    this.values = requireNonNull(values);
  }

  @Override
  public String qualification() {
    if (values.isEmpty()) return null;

    final String qualification = head(values).qualification();
    if (qualification == null) return null;

    if (values.stream().map(Value::qualification).allMatch(qualification::equals))
      return qualification;

    return null;
  }

  @Override
  public void setQualification(String qualification) {
    requireNonNull(qualification);
    values.forEach(it -> it.setQualification(qualification));
  }

  @Override
  public Value locate(Value target, PlanContext ctx) {
    final Value src0 = ctx.sourceOf(target);
    Value cached = null;
    for (Value v : values) {
      final Value src1;
      if (v == target || (strictEq(src0, src1 = ctx.sourceOf(v)))) return v;
      else if (relaxedEq(src0, src1)) cached = v;
    }
    return cached;
  }

  @Override
  public Value locate(String qualification, String name) {
    requireNonNull(name);

    for (Value value : values)
      if ((qualification == null || qualification.equals(value.qualification()))
          && name.equals(value.name())) return value;

    return null;
  }

  @Override
  public Value get(int index) {
    return values.get(index);
  }

  @Override
  public int size() {
    return values.size();
  }

  @Override
  public Iterator<Value> iterator() {
    return values.iterator();
  }

  @Override
  public Spliterator<Value> spliterator() {
    return values.spliterator();
  }

  private static boolean strictEq(Value v0, Value v1) {
    return v0 == v1;
  }

  private static boolean relaxedEq(Value v0, Value v1) {
    return v0.qualification().equals(v1.qualification()) && v0.name().equals(v1.name());
  }
}
