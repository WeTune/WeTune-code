package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY_SPEC;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public class DerivedAttribute extends BaseAttribute {
  // Subtleties:
  // 1. sometimes `reference` cannot be resolved beforehand,
  //    e.g. from x where (select x.a from y)
  //    such attribute is resolved lazily.
  // 2. sometimes `selectItem` is not available
  //    e.g. select * from t
  //    such attribute is resolve eagerly

  // invariant: reference == null => selectItem != null
  private final SQLNode selectItem;
  private Attribute reference;

  private DerivedAttribute(SQLNode selection, String name, Attribute ref) {
    super(selection.get(RELATION), name);
    this.reference = ref;
    this.selectItem = selection;
  }

  public static List<Attribute> projectionAttributesOf(SQLNode querySpec) {
    if (!QUERY_SPEC.isInstance(querySpec)) throw new IllegalArgumentException();

    final List<SQLNode> items = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final int estimatedSize =
        WILDCARD.isInstance(items.get(0).get(SELECT_ITEM_EXPR)) ? 8 : items.size();
    final List<Attribute> attributes = new ArrayList<>(estimatedSize);

    for (int i = 0; i < items.size(); i++) {
      final SQLNode item = items.get(i);
      if (WILDCARD.isInstance(item.get(SELECT_ITEM_EXPR))) expandWildcard(item, attributes);
      else attributes.add(new DerivedAttribute(item, selectItemName(item, i), null));
    }

    return attributes;
  }

  private static void expandWildcard(SQLNode item, List<Attribute> dest) {
    final Relation relation = item.get(RELATION);
    final SQLNode name = item.get(SELECT_ITEM_EXPR).get(WILDCARD_TABLE);
    final List<Relation> inputs =
        name == null
            ? relation.inputs()
            : singletonList(relation.input(name.get(TABLE_NAME_TABLE)));

    for (Relation input : inputs)
      for (Attribute ref : input.attributes())
        dest.add(new DerivedAttribute(item, ref.name(), ref));
  }

  private static String selectItemName(SQLNode selectItem, int index) {
    final String alias = selectItem.get(SELECT_ITEM_ALIAS);
    if (alias != null) return alias;

    final SQLNode expr = selectItem.get(SELECT_ITEM_EXPR);
    if (COLUMN_REF.isInstance(expr)) return expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN);

    return "item" + index;
  }

  @Override
  public Column column() {
    return null;
  }

  @Override
  public Attribute reference() {
    if (reference != null) return reference;

    assert selectItem != null;
    final SQLNode expr = selectItem.get(SELECT_ITEM_EXPR);
    return COLUMN_REF.isInstance(expr) ? (reference = Attribute.resolve(expr)) : null;
  }

  @Override
  public SQLNode node() {
    return selectItem;
  }
}