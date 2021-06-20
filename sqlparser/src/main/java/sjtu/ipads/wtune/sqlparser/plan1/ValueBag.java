package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

public interface ValueBag extends List<Value> {
  void setQualification(String qualification);

  static ValueBag empty() {
    return ValueBagImpl.EMPTY;
  }
}
