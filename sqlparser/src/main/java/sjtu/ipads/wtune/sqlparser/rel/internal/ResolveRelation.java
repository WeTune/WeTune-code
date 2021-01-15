package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public class ResolveRelation {
  public static void resolve(SQLNode node) {
    node.accept(SQLVisitor.traversal(ResolveRelation::resolveScope));
  }

  private static void resolveScope(SQLNode node) {
    final Relation rel;
    if (QUERY.isInstance(node) || SIMPLE_SOURCE.isInstance(node) || DERIVED_SOURCE.isInstance(node))
      rel = Relation.rootedBy(node);
    else rel = node.parent().get("sql.rel.relation");

    assert rel != null;
    node.put(RELATION, rel);
  }
}
