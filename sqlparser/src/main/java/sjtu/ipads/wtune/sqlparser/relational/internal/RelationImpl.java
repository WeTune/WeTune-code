package sjtu.ipads.wtune.sqlparser.relational.internal;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.tableSourceName;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY_SPEC;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SET_OP;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

public class RelationImpl implements Relation {
  private final ASTNode node;
  private final String alias;
  private final int digest;

  private List<Relation> inputs;
  private List<Attribute> attributes;

  private RelationImpl(ASTNode node) {
    this.node = node;
    this.alias = simpleName(tableSourceName(node));
    this.digest = node.hashCode();
  }

  public static Relation rootedBy(ASTNode node) {
    final ASTNode parent = node.parent();
    return DERIVED_SOURCE.isInstance(parent) && QUERY.isInstance(node)
        ? parent.get(RELATION)
        : new RelationImpl(node);
  }

  @Override
  public ASTNode node() {
    return node;
  }

  @Override
  public String alias() {
    return alias;
  }

  @Override
  public List<Relation> inputs() {
    if (SIMPLE_SOURCE.isInstance(node)) return emptyList();

    if (inputs == null) inputs = inputsOf(node);
    return inputs;
  }

  @Override
  public List<Attribute> attributes() {
    if (attributes == null) attributes = attributesOf(node);
    return attributes;
  }

  @Override
  public boolean isOutdated() {
    return node.hashCode() != digest;
  }

  @Override
  public String toString() {
    return node.toString();
  }

  private static List<Attribute> attributesOf(ASTNode node) {
    if (SIMPLE_SOURCE.isInstance(node)) {
      return Attribute.fromTable(node);

    } else if (QUERY_SPEC.isInstance(node)) {
      return Attribute.fromProjection(node);

    } else if (DERIVED_SOURCE.isInstance(node)) {
      return attributesOf(node.get(DERIVED_SUBQUERY));

    } else if (QUERY.isInstance(node)) {
      return attributesOf(node.get(QUERY_BODY));

    } else if (SET_OP.isInstance(node)) {
      return attributesOf(node.get(SET_OP_LEFT));

    } else throw new AssertionError();
  }

  private static List<Relation> inputsOf(ASTNode root) {
    final CollectInput collect = new CollectInput(root);
    root.accept(collect);
    return collect.inputs;
  }

  private static class CollectInput implements ASTVistor {
    private final ASTNode root;
    private final Relation rootRel;
    private final List<Relation> inputs = new ArrayList<>(4);

    private CollectInput(ASTNode root) {
      this.root = root;
      this.rootRel = root.get(RELATION);
    }

    @Override
    public boolean enter(ASTNode node) {
      if (node == root || !Relation.isRelationBoundary(node)) return true;

      final Relation rel = node.get(RELATION);
      if (rel == rootRel) return true;
      if (rel.isInput()) inputs.add(rel);
      return false;
    }
  }
}
