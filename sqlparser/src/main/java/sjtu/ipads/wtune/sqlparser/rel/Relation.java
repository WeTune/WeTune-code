package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.sqlparser.ast.AttrDomain;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func;
import static sjtu.ipads.wtune.sqlparser.ast.SQLVisitor.bottomUpVisit;
import static sjtu.ipads.wtune.sqlparser.ast.SQLVisitor.topDownVisit;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.rel.internal.RelationImpl.rootedBy;

public interface Relation {
  AttrDomain[] RELATION_BOUNDARY = {QUERY, SIMPLE_SOURCE, DERIVED_SOURCE};

  SQLNode node();

  String alias();

  List<Relation> inputs();

  List<Attribute> attributes();

  default Relation parent() {
    final SQLNode parentNode = node().parent();
    return parentNode == null ? null : parentNode.relation();
  }

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
    return DERIVED_SOURCE.isInstance(node()) ? null : parent();
  }

  static boolean isRelationBoundary(SQLNode node) {
    for (AttrDomain attrDomain : RELATION_BOUNDARY) if (attrDomain.isInstance(node)) return true;
    return false;
  }

  /**
   * Resolve relation for a node.
   *
   * <ol>
   *   Precondition:
   *   <li>node must be relation boundary.
   *   <li>the relation of node's parent must be resolved.
   * </ol>
   *
   * Advice: If you are not sure whether to call this method, then don't. Instead, attach the node
   * to AST properly, and call node.relation().
   */
  static Relation resolve(SQLNode node) {
    if (!isRelationBoundary(node))
      throw new IllegalArgumentException("cannot resolve relation for " + node.nodeType());
    // This check cannot be actually performed here, because parent().relation() triggers
    // cascading resolution, which is unintended:
    // ensure (node.parent() == null || node.parent().relation() != null)

    node.accept(topDownVisit(it -> it.setRelation(rootedBy(it)), RELATION_BOUNDARY));
    node.accept(bottomUpVisit(func(SQLNode::relation).then(Relation::attributes), QUERY));

    return node.relation();
  }
}
