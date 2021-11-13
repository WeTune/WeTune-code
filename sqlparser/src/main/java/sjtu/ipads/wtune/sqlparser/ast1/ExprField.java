package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.field.Fields;

import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.Expr_Kind;

class ExprField<T> extends SqlFieldBase<T> {
  private final ExprKind ownerKind;

  protected ExprField(String name, Class<?> type, ExprKind ownerKind) {
    super(ownerKind.name() + "." + name, type);
    this.ownerKind = ownerKind;
  }

  @Override
  public T getFrom(Fields target) {
    if (ownerKind != target.$(Expr_Kind)) return null;
    else return target.$(this);
  }

  @Override
  public void setTo(Fields target, T value) {
    checkValueType(value);
    if (ownerKind == target.$(Expr_Kind)) target.$(this, value);
  }
}
