package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeFieldBase;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public class RelationField extends NodeFieldBase<Relation> {
  public static final RelationField INSTANCE = new RelationField();

  protected RelationField() {
    super(SQL_ATTR_PREFIX + "rel.relation", Relation.class);
  }

  @Override
  public Relation get(Fields owner) {
    final ASTNode node = owner.unwrap(ASTNode.class);

    if (!Relation.isRelationBoundary(node)) {
      final ASTNode parent = node.parent();
      return parent == null ? null : parent.get(RELATION);
    }

    Relation relation = super.get(owner);
    if (relation == null) node.set(RELATION, relation = Relation.resolve(node));

    return relation;
  }
}
