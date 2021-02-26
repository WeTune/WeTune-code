package sjtu.ipads.wtune.sqlparser.relational;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.FieldDomain;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.relational.internal.AttributeField;
import sjtu.ipads.wtune.sqlparser.relational.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.relational.internal.NativeAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.GROUP_ITEM;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.ORDER_ITEM;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;

public interface Attribute {
  FieldKey<Attribute> ATTRIBUTE = AttributeField.INSTANCE;

  String name();

  Relation owner();

  ASTNode selectItem();

  Column column(boolean recursive);

  Attribute reference(boolean recursive);

  default ASTNode toColumnRef() {
    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, owner().alias());
    colName.set(COLUMN_NAME_COLUMN, name());
    final ASTNode columnRef = ASTNode.expr(COLUMN_REF);
    columnRef.set(COLUMN_REF_COLUMN, colName);
    return columnRef;
  }

  static Attribute resolve(ASTNode node) {
    if (!COLUMN_REF.isInstance(node)) return null;

    final Relation relation = node.get(RELATION);
    final ASTNode column = node.get(COLUMN_REF_COLUMN);
    final String tableName = column.get(COLUMN_NAME_TABLE);
    final String columnName = column.get(COLUMN_NAME_COLUMN);

    // !Resolution rule!
    // Take the enclosing relation of the node as reference, then
    // * Column in OrderBy will lookup in: output attributes -> input attributes
    // * Column in GroupBy will lookup in: input attributes -> output attributes
    // * Otherwise: output attributes

    Attribute attribute = null;

    if (tableName == null && isEnclosedBy(node, ORDER_ITEM))
      attribute = relation.attribute(columnName);

    if (attribute == null)
      if (tableName != null) attribute = relation.input(tableName).attribute(columnName);
      else
        for (Relation input : relation.inputs())
          if ((attribute = input.attribute(columnName)) != null) break;

    if (attribute == null && tableName == null && isEnclosedBy(node, GROUP_ITEM))
      attribute = relation.attribute(columnName);

    return attribute;
  }

  static List<Attribute> fromTable(ASTNode simpleTableSource) {
    return NativeAttribute.tableAttributesOf(simpleTableSource);
  }

  static List<Attribute> fromProjection(ASTNode querySpec) {
    return DerivedAttribute.projectionAttributesOf(querySpec);
  }

  static Attribute of(ASTNode node) {
    return node.get(ATTRIBUTE);
  }

  private static boolean isEnclosedBy(ASTNode node, FieldDomain type) {
    while (node.parent() != null) {
      if (type.isInstance(node)) return true;
      node = node.parent();
    }
    return false;
  }
}
