package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.internal.RelationImpl;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;

public interface Relation {
  Attrs.Key<Relation> RELATION = Attrs.key("sql.rel.relation", Relation.class);

  SQLNode node();

  String alias();

  Relation parent();

  List<Relation> inputs();

  List<Attribute> attributes();

  default Attribute attribute(String name) {
    for (Attribute attribute : attributes()) if (name.equals(attribute.name())) return attribute;
    return null;
  }

  default Relation input(String name) {
    for (Relation input : inputs()) if (name.equals(input.alias())) return input;
    final Relation aux = auxiliaryInput();
    return aux != null ? aux.input(name) : null;
  }

  default Relation auxiliaryInput() {
    if (DERIVED_SOURCE.isInstance(node())) return null;
    else return parent();
  }

  static Relation rootedBy(SQLNode node) {
    return RelationImpl.build(node);
  }

  static Relation of(SQLNode node) {
    return node.get(RELATION);
  }
}
