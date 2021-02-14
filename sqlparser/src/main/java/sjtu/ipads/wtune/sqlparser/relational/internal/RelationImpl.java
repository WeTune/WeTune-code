package sjtu.ipads.wtune.sqlparser.relational.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.tableSourceName;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;

public class RelationImpl implements Relation {
  private final ASTNode node;
  private final String alias;
  private final int digest;

  private List<Relation> inputs;
  private List<Attribute> attributes;

  private RelationImpl(ASTNode node) {
    this.node = node;
    this.alias = tableSourceName(node);
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
  //  private static void normalizeProjection(SQLNode querySpec) {
  //    final List<SQLNode> items = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
  //    if (items.stream().noneMatch(it -> WILDCARD.isInstance(it.get(SELECT_ITEM_EXPR)))) return;
  //
  //    final Relation rel = querySpec.relation();
  //    final List<SQLNode> newItems = new ArrayList<>(16);
  //    for (SQLNode item : items)
  //      if (!WILDCARD.isInstance(item.get(SELECT_ITEM_EXPR))) newItems.add(qualifyItem(rel,
  // item));
  //      else expandWildcard(rel, item, newItems);
  //
  //    querySpec.set(QUERY_SPEC_SELECT_ITEMS, newItems);
  //  }
  //
  //  private static SQLNode qualifyItem(Relation relation, SQLNode item) {
  //    final SQLNode column = item.get(SELECT_ITEM_EXPR).get(COLUMN_REF_COLUMN);
  //
  //    final String columnName = column.get(COLUMN_NAME_COLUMN);
  //    item.setIfAbsent(SELECT_ITEM_ALIAS, columnName);
  //
  //    if (column.get(COLUMN_NAME_TABLE) != null) return item;
  //
  //    for (Relation input : relation.inputs())
  //      if (input.attribute(columnName) != null) {
  //        column.set(COLUMN_NAME_TABLE, input.alias());
  //        break;
  //      }
  //
  //    return item;
  //  }
  //
  //  private static void expandWildcard(Relation relation, SQLNode item, List<SQLNode> dest) {
  //    final SQLNode name = item.get(SELECT_ITEM_EXPR).get(WILDCARD_TABLE);
  //    final List<Relation> inputs =
  //        name == null
  //            ? relation.inputs()
  //            : singletonList(relation.input(name.get(TABLE_NAME_TABLE)));
  //
  //    for (Relation input : inputs)
  //      for (Attribute attribute : input.attributes()) dest.add(attribute.toSelectItem());
  //  }
  //
}
