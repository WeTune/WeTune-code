package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.function.Function;

import static java.util.Collections.singletonList;

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
    final Expr exprCopy = expr().copy();
    exprCopy.setRefs(RefBag.mk(ListSupport.map((Iterable<Ref>) exprCopy.refs(), (Function<? super Ref, ? extends Ref>) RefImpl::new)));
    return mk(qualification(), name(), exprCopy);
  }

  static Value mk(String qualification, String name, Expr expr) {
    final ExprValue v = new ExprValue(name, expr);
    v.setQualification(qualification);
    return v;
  }
}
