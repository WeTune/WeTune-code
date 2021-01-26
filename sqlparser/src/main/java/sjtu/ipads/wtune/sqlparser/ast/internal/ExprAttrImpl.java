package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.internal.NodeAttrImpl.SQL_ATTR_PREFIX;

public class ExprAttrImpl<T> extends NodeAttrBase<T> {
  private final ExprType type;

  private ExprAttrImpl(ExprType type, String name, Class<?> targetClass) {
    super(name, targetClass);
    this.type = type;
  }

  public static <T> AttrKey<T> build(ExprType type, String name, Class<?> targetClass) {
    requireNonNull(type);
    requireNonNull(name);
    requireNonNull(targetClass);

    return new ExprAttrImpl<>(
        type, SQL_ATTR_PREFIX + "expr." + type.name().toLowerCase() + "." + name, targetClass);
  }

  @Override
  public boolean validate(Attrs owner, Object obj) {
    if (super.validate(owner, obj)) {
      final SQLNode node = owner.unwrap(SQLNode.class);
      return EXPR.isInstance(node) && type.isInstance(node);
    }
    return false;
  }
}
