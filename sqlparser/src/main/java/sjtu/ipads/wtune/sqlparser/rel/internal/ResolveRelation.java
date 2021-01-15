package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func;
import static sjtu.ipads.wtune.sqlparser.ast.SQLVisitor.bottomUpVisit;
import static sjtu.ipads.wtune.sqlparser.ast.SQLVisitor.topDownVisit;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public class ResolveRelation {
  public static void resolve(SQLNode node) {
    node.accept(topDownVisit(ResolveRelation::resolveScope));
    node.accept(bottomUpVisit(QUERY, func(Relation::of).then(Relation::attributes)));
    node.accept(bottomUpVisit(COLUMN_REF, Attribute::resolve));
  }

  private static void resolveScope(SQLNode node) {
    final Relation rel;
    if (QUERY.isInstance(node) || SIMPLE_SOURCE.isInstance(node) || DERIVED_SOURCE.isInstance(node))
      rel = Relation.rootedBy(node);
    else rel = Relation.of(node.parent());

    assert rel != null;
    node.put(RELATION, rel);
  }
}
