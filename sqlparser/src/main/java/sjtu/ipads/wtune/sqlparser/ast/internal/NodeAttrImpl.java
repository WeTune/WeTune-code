package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import static java.util.Objects.requireNonNull;

public class NodeAttrImpl<T> extends NodeAttrBase<T> {
  private final NodeType type;

  private NodeAttrImpl(NodeType type, String name, Class<?> targetClass) {
    super(name, targetClass);
    this.type = type;
  }

  public static <T> AttrKey<T> build(NodeType type, String name, Class<?> targetClass) {
    requireNonNull(type);
    requireNonNull(name);
    requireNonNull(targetClass);

    return new NodeAttrImpl<>(
        type, SQL_ATTR_PREFIX + type.name().toLowerCase() + "." + name, targetClass);
  }

  @Override
  public boolean validate(Attrs owner, Object obj) {
    return super.validate(owner, obj) && owner.unwrap(SQLNode.class).nodeType() == type;
  }
}
