package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.internal.RelationImpl;

import java.util.List;

public interface Relation {
  Attrs.Key<Relation> RELATION = Attrs.key("sql.rel.relation", Relation.class);

  SQLNode node();

  String alias();

  Relation parent();

  List<Relation> inputs();

  List<Attribute> attributes();

  void addInput(Relation relation);

  static Relation rootedBy(SQLNode root) {
    return RelationImpl.build(root);
  }
}
