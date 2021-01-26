package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;

public class TableSourceAttrImpl<T> extends NodeAttrBase<T> {
  private final TableSourceType type;

  private TableSourceAttrImpl(TableSourceType type, String name, Class<?> targetClass) {
    super(name, targetClass);
    this.type = type;
  }

  public static <T> AttrKey<T> build(TableSourceType type, String name, Class<?> targetClass) {
    requireNonNull(type);
    requireNonNull(name);
    requireNonNull(targetClass);

    return new TableSourceAttrImpl<>(
        type, SQL_ATTR_PREFIX + "tableSource." + type.name().toLowerCase() + name, targetClass);
  }

  @Override
  public boolean validate(Attrs owner, Object obj) {
    if (super.validate(owner, obj)) {
      final SQLNode node = owner.unwrap(SQLNode.class);
      return TABLE_SOURCE.isInstance(node) && type.isInstance(node);
    }
    return false;
  }
}
