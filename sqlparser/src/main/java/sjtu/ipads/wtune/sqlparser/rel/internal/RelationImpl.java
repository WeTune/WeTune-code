package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.tableSourceName;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;

public class RelationImpl implements Relation {
  private final SQLNode node;
  private final String alias;
  private final Relation parent;
  private final List<Relation> inputs;

  private List<Attribute> attributes;

  private RelationImpl(SQLNode node, Relation parent, int expectedNumInputs) {
    this.node = node;
    this.alias = tableSourceName(node);
    this.parent = parent;
    this.inputs = expectedNumInputs == 0 ? emptyList() : new ArrayList<>(expectedNumInputs);
  }

  public static Relation build(SQLNode node) {
    final Relation relation = Relation.of(node);
    if (relation != null) return relation;

    final SQLNode parentNode = node.parent();
    final Relation parent = parentNode == null ? null : Relation.of(parentNode);

    if (DERIVED_SOURCE.isInstance(parentNode) && QUERY.isInstance(node)) return parent;

    final int expectedNumInputs = SIMPLE_SOURCE.isInstance(node) ? 0 : 4;
    final Relation rel = new RelationImpl(node, parent, expectedNumInputs);

    if (parent != null && (TABLE_SOURCE.isInstance(node) || SET_OP.isInstance(parentNode)))
      parent.inputs().add(rel);

    return rel;
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
  public Relation parent() {
    return parent;
  }

  @Override
  public List<Relation> inputs() {
    return inputs;
  }

  @Override
  public List<Attribute> attributes() {
    if (attributes != null) return attributes;
    return attributes = outputAttributesOf(node);
  }

  private List<Attribute> outputAttributesOf(SQLNode node) {
    if (SIMPLE_SOURCE.isInstance(node)) {
      return Attribute.fromTable(node);

    } else if (QUERY_SPEC.isInstance(node)) {
      normalizeProjection(node);
      return Attribute.fromProjection(node);

    } else if (DERIVED_SOURCE.isInstance(node)) {
      return outputAttributesOf(node.get(DERIVED_SUBQUERY));

    } else if (QUERY.isInstance(node)) {
      return outputAttributesOf(node.get(QUERY_BODY));

    } else if (SET_OP.isInstance(node)) {
      return outputAttributesOf(node.get(SET_OP_LEFT));

    } else throw new AssertionError();
  }

  private static void normalizeProjection(SQLNode querySpec) {
    final List<SQLNode> items = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    if (items.stream().noneMatch(it -> WILDCARD.isInstance(it.get(SELECT_ITEM_EXPR)))) return;

    final Relation rel = Relation.of(querySpec);
    final List<SQLNode> newItems = new ArrayList<>(16);
    for (SQLNode item : items)
      if (!WILDCARD.isInstance(item.get(SELECT_ITEM_EXPR))) newItems.add(qualifyItem(rel, item));
      else expandWildcard(rel, item, newItems);

    querySpec.put(QUERY_SPEC_SELECT_ITEMS, newItems);
  }

  private static SQLNode qualifyItem(Relation relation, SQLNode item) {
    final SQLNode column = item.get(SELECT_ITEM_EXPR).get(COLUMN_REF_COLUMN);

    final String columnName = column.get(COLUMN_NAME_COLUMN);
    item.putIfAbsent(SELECT_ITEM_ALIAS, columnName);

    if (column.get(COLUMN_NAME_TABLE) != null) return item;

    for (Relation input : relation.inputs())
      if (input.attribute(columnName) != null) {
        column.put(COLUMN_NAME_TABLE, input.alias());
        break;
      }

    return item;
  }

  private static void expandWildcard(Relation relation, SQLNode item, List<SQLNode> dest) {
    final SQLNode name = item.get(SELECT_ITEM_EXPR).get(WILDCARD_TABLE);
    final List<Relation> inputs =
        name == null
            ? relation.inputs()
            : singletonList(relation.input(name.get(TABLE_NAME_TABLE)));

    for (Relation input : inputs)
      for (Attribute attribute : input.attributes()) dest.add(attribute.toSelectItem());
  }
}
