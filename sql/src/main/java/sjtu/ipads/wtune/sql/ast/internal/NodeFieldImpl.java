package sjtu.ipads.wtune.sql.ast.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.NodeFields;
import sjtu.ipads.wtune.sql.ast.constants.NodeType;

import static java.util.Objects.requireNonNull;

public class NodeFieldImpl<T> extends NodeFieldBase<T> implements NodeFields {
  private final NodeType type;

  private NodeFieldImpl(NodeType type, String name, Class<?> targetClass) {
    super(name, targetClass);
    this.type = type;
  }

  public static <T> FieldKey<T> build(NodeType type, String name, Class<?> targetClass) {
    requireNonNull(type);
    requireNonNull(name);
    requireNonNull(targetClass);

    return new NodeFieldImpl<>(
        type, SQL_ATTR_PREFIX + type.name().toLowerCase() + "." + name, targetClass);
  }

  @Override
  public boolean validate(Fields owner, Object obj) {
    return super.validate(owner, obj) && owner.unwrap(ASTNode.class).nodeType() == type;
  }
}
