package sjtu.ipads.wtune.sql.relational;

import static sjtu.ipads.wtune.sql.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sql.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sql.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sql.ast.NodeFields.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sql.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.GROUP_ITEM;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.ORDER_ITEM;
import static sjtu.ipads.wtune.sql.relational.Relation.RELATION;
import static sjtu.ipads.wtune.sql.util.ASTHelper.isEnclosedBy;
import static sjtu.ipads.wtune.sql.util.ASTHelper.isParentedBy;

import java.util.List;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.relational.internal.AttributeField;
import sjtu.ipads.wtune.sql.relational.internal.DerivedAttribute;
import sjtu.ipads.wtune.sql.relational.internal.NativeAttribute;
import sjtu.ipads.wtune.sql.schema.Column;

public interface Attribute {
  FieldKey<Attribute> ATTRIBUTE = AttributeField.INSTANCE;

  String name();

  Relation owner();

  ASTNode selectItem();

  Column column(boolean recursive);

  Attribute reference(boolean recursive);

  static Attribute resolve(ASTNode node) {
    if (!COLUMN_REF.isInstance(node)) return null;

    final Relation relation = node.get(RELATION);
    final ASTNode column = node.get(COLUMN_REF_COLUMN);
    final String tableName = column.get(COLUMN_NAME_TABLE);
    final String columnName = column.get(COLUMN_NAME_COLUMN);

    // !Resolution Rule!
    // Take the enclosing relation of the node as reference, then
    // * Column in OrderBy will lookup in: output attributes -> input attributes
    // * Column in GroupBy will lookup in: input attributes -> output attributes
    // * Otherwise: output attributes

    Attribute attribute = null;

    if (tableName == null && isEnclosedBy(node, ORDER_ITEM))
      attribute = relation.attribute(columnName);

    if (attribute == null)
      if (tableName != null) {
        final Relation table = relation.input(tableName);
        if (table == null) return null;

        attribute = table.attribute(columnName);

      } else
        for (Relation input : relation.inputs())
          if ((attribute = input.attribute(columnName)) != null) break;

    if (attribute == null
        && tableName == null
        && (isEnclosedBy(node, GROUP_ITEM) || isParentedBy(node, QUERY_SPEC_HAVING)))
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
}