package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.*;

import static java.util.Objects.requireNonNull;

class ValueBagImpl extends AbstractList<Value> implements ValueBag {
  private final List<Value> values;

  static final ValueBag EMPTY = new ValueBagImpl(Collections.emptyList());

  ValueBagImpl(List<Value> values) {
    this.values = requireNonNull(values);
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
