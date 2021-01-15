package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Column;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.QUERY_SPEC_SELECT_ITEMS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY_SPEC;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;

public class DerivedAttribute extends BaseAttribute {
  private final SQLNode node;

  private DerivedAttribute(SQLNode node) {
    super(Relation.of(node), node.get(SELECT_ITEM_ALIAS));
    this.node = node;
  }

  public static Attribute build(SQLNode node) {
    if (!SELECT_ITEM.isInstance(node)) throw new IllegalArgumentException();
    return new DerivedAttribute(node);
  }

  public static List<Attribute> projectionAttributesOf(SQLNode querySpec) {
    if (!QUERY_SPEC.isInstance(querySpec)) throw new IllegalArgumentException();
    return listMap(DerivedAttribute::new, querySpec.get(QUERY_SPEC_SELECT_ITEMS));
  }

  @Override
  public Column column() {
    return null;
  }

  @Override
  public SQLNode node() {
    return node;
  }

  @Override
  public String name() {
    return node.get(SELECT_ITEM_ALIAS);
  }
}
