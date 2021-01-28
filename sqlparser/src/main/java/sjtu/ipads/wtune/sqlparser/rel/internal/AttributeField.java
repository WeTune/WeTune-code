package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeFieldBase;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;

public class AttributeField extends NodeFieldBase<Attribute> {
  public static final AttributeField INSTANCE = new AttributeField();

  protected AttributeField() {
    super(SQL_ATTR_PREFIX + "rel.attribute", Attribute.class);
  }

  @Override
  public Attribute get(Fields owner) {
    return Attribute.resolve(owner.unwrap(SQLNode.class));
  }
}
