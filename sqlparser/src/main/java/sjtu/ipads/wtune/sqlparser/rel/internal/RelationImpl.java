package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SET_OP_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.tableSourceName;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;

public class RelationImpl implements Relation {
  private final SQLNode node;
  private final String alias;

  private List<Relation> inputs;
  private List<Attribute> attributes;
  private int expectedVersion;

  private RelationImpl(SQLNode node) {
    this.node = node;
    this.alias = tableSourceName(node);
  }

  public static Relation rootedBy(SQLNode node) {
    final SQLNode parent = node.parent();
    return DERIVED_SOURCE.isInstance(parent) && QUERY.isInstance(node)
        ? parent.get(RELATION)
        : new RelationImpl(node);
  }

  @Override
  public SQLNode node() {
    return node;
  }

  @Override
  public String alias() {
    return alias;
  }

  @Override
  public List<Relation> inputs() {
    if (SIMPLE_SOURCE.isInstance(node)) return emptyList();

    final SQLContext ctx = node.context();
    if (inputs == null || (ctx != null && ctx.versionNumber() != expectedVersion)) {
      expectedVersion = ctx == null ? 0 : ctx.versionNumber();
      inputs = inputsOf(node);
    }

    return inputs;
  }

  @Override
  public List<Attribute> attributes() {
    final SQLContext ctx = node.context();
    if (attributes == null || (ctx != null && ctx.versionNumber() != expectedVersion)) {
      expectedVersion = ctx == null ? 0 : ctx.versionNumber();
      attributes = outputAttributesOf(node);
    }

    return attributes;
  }

  @Override
  public void reset() {
    inputs = null;
    attributes = null;

    if (!isInput()) return;

    final Relation parent = parent();
    if (parent != null) parent.reset();
  }

  private static List<Attribute> outputAttributesOf(SQLNode node) {
    if (SIMPLE_SOURCE.isInstance(node)) {
      return Attribute.fromTable(node);

    } else if (QUERY_SPEC.isInstance(node)) {
      return Attribute.fromProjection(node);

    } else if (DERIVED_SOURCE.isInstance(node)) {
      return outputAttributesOf(node.get(DERIVED_SUBQUERY));

    } else if (QUERY.isInstance(node)) {
      return outputAttributesOf(node.get(QUERY_BODY));

    } else if (SET_OP.isInstance(node)) {
      return outputAttributesOf(node.get(SET_OP_LEFT));

    } else throw new AssertionError();
  }

  private static List<Relation> inputsOf(SQLNode root) {
    final CollectInput collect = new CollectInput(root);
    root.accept(collect);
    return collect.inputs;
  }

  private static class CollectInput implements SQLVisitor {
    private final SQLNode root;
    private final Relation rootRel;
    private final List<Relation> inputs = new ArrayList<>(4);

    private CollectInput(SQLNode root) {
      this.root = root;
      this.rootRel = root.get(RELATION);
    }

    @Override
    public boolean enter(SQLNode node) {
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
