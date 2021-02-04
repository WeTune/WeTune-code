package sjtu.ipads.wtune.sqlparser.ast.constants;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.FieldDomain;
import sjtu.ipads.wtune.sqlparser.ast.internal.TableSourceFieldImpl;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_SOURCE_KIND;

public enum TableSourceType implements FieldDomain {
  SIMPLE_SOURCE,
  JOINED,
  DERIVED_SOURCE;

  @Override
  public boolean isInstance(ASTNode node) {
    return node != null && node.get(TABLE_SOURCE_KIND) == this;
  }

  public boolean isJoined() {
    return this == JOINED;
  }

  public boolean isSimple() {
    return this == SIMPLE_SOURCE;
  }

  public boolean isDerived() {
    return this == DERIVED_SOURCE;
  }

  @Override
  public <T, R extends T> FieldKey<R> attr(String name, Class<T> clazz) {
    return TableSourceFieldImpl.build(this, name, clazz);
  }
}
