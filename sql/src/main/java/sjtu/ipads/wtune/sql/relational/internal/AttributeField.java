package sjtu.ipads.wtune.sql.relational.internal;

import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.internal.NodeFieldBase;
import sjtu.ipads.wtune.sql.relational.Attribute;

public class AttributeField extends NodeFieldBase<Attribute> {
  public static final AttributeField INSTANCE = new AttributeField();

  protected AttributeField() {
    super(SQL_ATTR_PREFIX + "rel.attribute", Attribute.class);
  }

  @Override
  public Attribute get(Fields owner) {
    return Attribute.resolve(owner.unwrap(ASTNode.class));
  }
}
