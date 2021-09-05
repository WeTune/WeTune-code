package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;

public interface ValueBag extends List<Value> {
  // if all values shares a common qualification, returns that,
  // otherwise, returns null (if the bag is empty, also returns null).
  // The complexity is O(n), n is the bag's size.
  String qualification();

  void setQualification(String qualification);

  static Value locateValue(List<Value> values, Value target, PlanContext ctx) {
    final Value redirected = ctx.redirect(target);
    return values.contains(redirected) ? redirected : null;
  }

  static Value locateValue(List<Value> values, String qualification, String name) {
    requireNonNull(name);

    for (Value value : values)
      if ((qualification == null || qualification.equals(value.qualification()))
          && name.equals(value.name())) return value;

    return null;
  }

  static Value locateValueRelaxed(List<Value> values, Value target, PlanContext ctx) {
    final Value src0 = coalesce(ctx.sourceOf(target), target);
    Value cached = null;
    for (Value v : values) {
      Value src1;
      if (v == target || strictEq(src0, src1 = ctx.sourceOf(v))) return v;
      if (relaxedEq(src0, src1)) cached = v;
    }
    return cached;
  }

  static ValueBag empty() {
    return ValueBagImpl.EMPTY;
  }

  static ValueBag mk(List<Value> values) {
    if (values instanceof ValueBag) return (ValueBag) values;
    else return new ValueBagImpl(values);
  }

  private static boolean strictEq(Value v0, Value v1) {
    return v0 == v1;
  }

  private static boolean relaxedEq(Value v0, Value v1) {
    return v0 != null && v0.column() != null && v1 != null && v0.column().equals(v1.column());
  }
}
