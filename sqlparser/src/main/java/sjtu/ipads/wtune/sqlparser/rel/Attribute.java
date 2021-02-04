package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.rel.internal.AttributeField;
import sjtu.ipads.wtune.sqlparser.rel.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.rel.internal.NativeAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public interface Attribute {
  FieldKey<Attribute> ATTRIBUTE = AttributeField.INSTANCE;

  String name();

  Relation owner();

  Attribute reference();

  Column column();

  ASTNode node();

  default Attribute reference(boolean recursive) {
    if (!recursive) return this;

    final Attribute ref = reference();
    if (ref == null) return this;
    return ref.reference(true);
  }

  default Column column(boolean recursive) {
    final Column column = column();

    if (!recursive || column != null) return column;
    else if (reference() != null) return reference().column(true);
    else return null;
  }

  default ASTNode toSelectItem() {
    final ASTNode columnName = ASTNode.node(NodeType.COLUMN_NAME);
    final Relation owner = owner();
    columnName.set(COLUMN_NAME_TABLE, owner.alias());
    columnName.set(COLUMN_NAME_COLUMN, name());

    final ASTNode columnRef = ASTNode.expr(COLUMN_REF);
    columnRef.set(COLUMN_REF_COLUMN, columnName);

    final ASTNode selectItem = ASTNode.node(NodeType.SELECT_ITEM);
    selectItem.set(SELECT_ITEM_ALIAS, name());
    selectItem.set(SELECT_ITEM_EXPR, columnRef);

    return selectItem;
  }

  static Attribute resolve(ASTNode node) {
    if (!COLUMN_REF.isInstance(node)) return null;

    final Relation relation = node.get(RELATION);
    final ASTNode column = node.get(COLUMN_REF_COLUMN);
    final String tableName = column.get(COLUMN_NAME_TABLE);
    final String columnName = column.get(COLUMN_NAME_COLUMN);

    Attribute attribute = null;
    if (tableName != null) attribute = relation.input(tableName).attribute(columnName);
    else
      for (Relation input : relation.inputs())
        if ((attribute = input.attribute(columnName)) != null) break;

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
