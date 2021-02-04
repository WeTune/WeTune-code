package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;

public class ExprFieldImpl<T> extends NodeFieldBase<T> {
  private final ExprType type;

  private ExprFieldImpl(ExprType type, String name, Class<?> targetClass) {
    super(name, targetClass);
    this.type = type;
  }

  public static <T> FieldKey<T> build(ExprType type, String name, Class<?> targetClass) {
    requireNonNull(type);
    requireNonNull(name);
    requireNonNull(targetClass);

    return new ExprFieldImpl<>(
        type, SQL_ATTR_PREFIX + "expr." + type.name().toLowerCase() + "." + name, targetClass);
  }

  @Override
  public boolean validate(Fields owner, Object obj) {
    if (super.validate(owner, obj)) {
      final ASTNode node = owner.unwrap(ASTNode.class);
      return EXPR.isInstance(node) && type.isInstance(node);
    }
    return false;
  }
}
