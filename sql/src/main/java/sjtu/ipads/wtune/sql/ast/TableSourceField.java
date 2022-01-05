package sjtu.ipads.wtune.sql.ast;

import sjtu.ipads.wtune.common.field.Fields;

import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.TableSource_Kind;

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
  public T setTo(Fields target, T value) {
    checkValueType(value);
    if (ownerKind == target.$(TableSource_Kind)) return target.$(this, value);
    throw new IllegalArgumentException(
        "cannot set field. %s %s".formatted(name(), target.$(TableSource_Kind)));
  }
}
