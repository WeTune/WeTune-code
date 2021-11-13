package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.field.Fields;

import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.TableSource_Kind;

class TableSourceField<T> extends SqlFieldBase<T> {
  private final TableSourceKind ownerKind;

  protected TableSourceField(String name, Class<?> type, TableSourceKind ownerKind) {
    super(name, type);
    this.ownerKind = ownerKind;
  }

  @Override
  public T getFrom(Fields target) {
    if (ownerKind != target.$(TableSource_Kind)) return null;
    else return target.$(this);
  }

  @Override
  public void setTo(Fields target, T value) {
    checkValueType(value);
    if (ownerKind == target.$(TableSource_Kind)) target.$(this, value);
  }
}
