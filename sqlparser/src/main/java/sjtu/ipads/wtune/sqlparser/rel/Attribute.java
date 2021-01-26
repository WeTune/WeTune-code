package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.rel.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.rel.internal.NativeAttribute;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;

public interface Attribute {
  AttrKey<Attribute> ATTRIBUTE = AttrKey.checked("sql.rel.attribute", Attribute.class);

  String name();

  Relation owner();

  SQLNode node();

  Column column();

  default Column column(boolean recursive) {
    final Column column = column();

    if (!recursive || column != null) return column;
    else if (node() != null) return node().get(ATTRIBUTE).column(true);
    else return null;
  }

  default SQLNode toSelectItem() {
    final SQLNode columnName = SQLNode.simple(NodeType.COLUMN_NAME);
    final Relation owner = owner();
    columnName.set(COLUMN_NAME_TABLE, owner.alias());
    columnName.set(COLUMN_NAME_COLUMN, name());

    final SQLNode columnRef = SQLNode.simple(COLUMN_REF);
    columnRef.set(COLUMN_REF_COLUMN, columnName);

    final SQLNode selectItem = SQLNode.simple(NodeType.SELECT_ITEM);
    selectItem.set(SELECT_ITEM_ALIAS, name());
    selectItem.set(SELECT_ITEM_EXPR, columnRef);

    return selectItem;
  }

  static Attribute resolve(SQLNode node) {
    if (!COLUMN_REF.isInstance(node)) throw new IllegalArgumentException();

    final Relation relation = node.relation();
    final SQLNode column = node.get(COLUMN_REF_COLUMN);
    final String tableName = column.get(COLUMN_NAME_TABLE);
    final String columnName = column.get(COLUMN_NAME_COLUMN);

    Attribute attribute = null;
    if (tableName != null) attribute = relation.input(tableName).attribute(columnName);
    else
      for (Relation input : relation.inputs())
        if ((attribute = input.attribute(columnName)) != null) break;

    return attribute;
  }

  static List<Attribute> fromTable(SQLNode simpleTableSource) {
    return NativeAttribute.tableAttributesOf(simpleTableSource);
  }

  static List<Attribute> fromProjection(SQLNode querySpec) {
    return DerivedAttribute.projectionAttributesOf(querySpec);
  }

  static Attribute of(SQLNode node) {
    return node.get(ATTRIBUTE);
  }
}
