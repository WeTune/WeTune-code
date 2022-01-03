package sjtu.ipads.wtune.sql.ast.constants;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.FieldDomain;
import sjtu.ipads.wtune.sql.ast.internal.TableSourceFieldImpl;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sql.ast.NodeFields.TABLE_SOURCE_KIND;

public enum TableSourceKind implements FieldDomain {
  SIMPLE_SOURCE,
  JOINED_SOURCE,
  DERIVED_SOURCE;

  private final List<FieldKey> fields = new ArrayList<>(5);

  @Override
  public List<FieldKey> fields() {
    return fields;
  }

  @Override
  public boolean isInstance(ASTNode node) {
    return node != null && node.get(TABLE_SOURCE_KIND) == this;
  }

  @Override
  public <T, R extends T> FieldKey<R> attr(String name, Class<T> clazz) {
    final FieldKey<R> field = TableSourceFieldImpl.build(this, name, clazz);
    fields.add(field);
    return field;
  }
}
