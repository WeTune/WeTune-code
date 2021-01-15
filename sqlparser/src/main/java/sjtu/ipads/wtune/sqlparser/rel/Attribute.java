package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.rel.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.rel.internal.NativeAttribute;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public interface Attribute {
  Attrs.Key<Attribute> ATTRIBUTE = Attrs.key("sql.rel.attribute", Attribute.class);

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
    columnName.put(COLUMN_NAME_TABLE, owner.alias());
    columnName.put(COLUMN_NAME_COLUMN, name());

    final SQLNode columnRef = SQLNode.simple(COLUMN_REF);
    columnRef.put(COLUMN_REF_COLUMN, columnName);

    final SQLNode selectItem = SQLNode.simple(NodeType.SELECT_ITEM);
    selectItem.put(SELECT_ITEM_ALIAS, name());
    selectItem.put(SELECT_ITEM_EXPR, columnRef);

    final Relation scope = owner.parent();
    columnName.put(RELATION, scope);
    columnRef.put(RELATION, scope);
    selectItem.put(RELATION, scope);
    return selectItem;
  }

  static void resolve(SQLNode node) {
    if (!COLUMN_REF.isInstance(node)) throw new IllegalArgumentException();

    final Relation relation = Relation.of(node);
    final SQLNode column = node.get(COLUMN_REF_COLUMN);
    final String tableName = column.get(COLUMN_NAME_TABLE);
    final String columnName = column.get(COLUMN_NAME_COLUMN);

    Attribute attribute = null;
    if (tableName != null) attribute = relation.input(tableName).attribute(columnName);
    else
      for (Relation input : relation.inputs())
        if ((attribute = input.attribute(columnName)) != null) break;

    node.put(ATTRIBUTE, attribute);
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
