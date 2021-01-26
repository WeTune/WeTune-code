package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeAttrBase;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

public class RelationAttr extends NodeAttrBase<Relation> {
  protected RelationAttr() {
    super(SQL_ATTR_PREFIX + "rel.relation", Relation.class);
  }

  @Override
  public Relation get(Attrs owner) {
    final Relation relation = super.get(owner);
    if (relation == null) return null;
    else return relation;
  }
}
