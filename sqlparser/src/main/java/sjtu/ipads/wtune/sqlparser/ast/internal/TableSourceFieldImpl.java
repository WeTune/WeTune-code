package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;

public class TableSourceFieldImpl<T> extends NodeFieldBase<T> {
  private final TableSourceType type;

  private TableSourceFieldImpl(TableSourceType type, String name, Class<?> targetClass) {
    super(name, targetClass);
    this.type = type;
  }

  public static <T> FieldKey<T> build(TableSourceType type, String name, Class<?> targetClass) {
    requireNonNull(type);
    requireNonNull(name);
    requireNonNull(targetClass);

    return new TableSourceFieldImpl<>(
        type, SQL_ATTR_PREFIX + "tableSource." + type.name().toLowerCase() + name, targetClass);
  }

  @Override
  public boolean validate(Fields owner, Object obj) {
    if (super.validate(owner, obj)) {
      final ASTNode node = owner.unwrap(ASTNode.class);
      return TABLE_SOURCE.isInstance(node) && type.isInstance(node);
    }
    return false;
  }
}
