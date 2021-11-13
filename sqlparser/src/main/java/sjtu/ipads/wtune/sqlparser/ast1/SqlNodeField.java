package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.field.Fields;
import sjtu.ipads.wtune.common.tree.AstFields;

class SqlNodeField<T> extends SqlFieldBase<T> {
  private final SqlKind ownerKind;

  protected SqlNodeField(String name, Class<?> type, SqlKind ownerKind) {
    super(ownerKind.name() + "." + name, type);
    this.ownerKind = ownerKind;
  }

  @Override
  public T getFrom(Fields target) {
    if (ownerKind != ((AstFields) target).kind()) return null;
    else return target.$(this);
  }

  @Override
  public void setTo(Fields target, T value) {
    checkValueType(value);
    if (ownerKind == ((AstFields) target).kind()) target.$(this, value);
  }
}
