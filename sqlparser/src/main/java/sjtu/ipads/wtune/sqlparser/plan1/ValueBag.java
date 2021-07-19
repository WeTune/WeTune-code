package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

public interface ValueBag extends List<Value> {
  // if all values shares a common qualification, returns that,
  // otherwise, returns null (if the bag is empty, also returns null).
  // The complexity is O(n), n is the bag's size.
  String qualification();

  void setQualification(String qualification);

  static ValueBag empty() {
    return ValueBagImpl.EMPTY;
  }

  static ValueBag mk(List<Value> values) {
    if (values instanceof ValueBag) return (ValueBag) values;
    else return new ValueBagImpl(values);
  }
}
