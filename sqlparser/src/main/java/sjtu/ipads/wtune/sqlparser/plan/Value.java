package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.schema.Column;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public interface Value {
  String qualification();

  String name();

  // the following three are exclusive:
  // if column() != null, then expr() == null && wildcardQualification() == null
  // and so on.
  // All three are null is permitted, which indicates the value is an unqualified wildcard,
  // i.e., a plain "*".
  Column column();

  Expr expr();

  String wildcardQualification();

  void setQualification(String qualification);

  void setName(String name); // BE CAUTION INVOKING THIS! (intended only used when prove a rule)

  default Ref selfish() {
    return new RefImpl(qualification(), name());
  }

  default Value wrapAsExprValue() {
    return mk(qualification(), name(), Expr.mk(RefBag.mk(singletonList(selfish()))));
  }

  default Value renewRefs() {
    return mk(qualification(), name(), Expr.mk(RefBag.mk(listMap(expr().refs(), RefImpl::new))));
  }

  static Value mk(String qualification, String name, Expr expr) {
    final ExprValue v = new ExprValue(name, expr);
    v.setQualification(qualification);
    return v;
  }
}
