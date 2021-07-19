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
}
