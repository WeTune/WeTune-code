package sjtu.ipads.wtune.sqlparser.relational;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.relational.internal.AttributeField;
import sjtu.ipads.wtune.sqlparser.relational.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.relational.internal.NativeAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.COLUMN_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;

public interface Attribute {
  FieldKey<Attribute> ATTRIBUTE = AttributeField.INSTANCE;

  String name();

  Relation owner();

  Attribute reference();

  Column column();

  ASTNode node();

  default Attribute reference(boolean recursive) {
    final Attribute ref = reference();
    if (ref == null) return this;
    if (!recursive) return ref;
    return ref.reference(true);
  }

  default Column column(boolean recursive) {
    final Column column = column();

    if (!recursive || column != null) return column;
    else if (reference() != null) return reference().column(true);
    else return null;
  }

  default ASTNode toColumnRef() {
    final ASTNode columnName = ASTNode.node(COLUMN_NAME);
    final Relation owner = owner();
    columnName.set(COLUMN_NAME_TABLE, owner.alias());
    columnName.set(COLUMN_NAME_COLUMN, name());

    final ASTNode columnRef = ASTNode.expr(COLUMN_REF);
    columnRef.set(COLUMN_REF_COLUMN, columnName);

    return columnRef;
  }

  default ASTNode toSelectItem() {
    final Attribute ref = reference();
    if (ref == null) return node().copy();

    final ASTNode colName = ASTNode.node(COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, ref.owner().alias());
    colName.set(COLUMN_NAME_COLUMN, ref.name());

    final ASTNode columnRef = ASTNode.expr(COLUMN_REF);
    columnRef.set(COLUMN_REF_COLUMN, colName);

    final ASTNode selectItem = ASTNode.node(SELECT_ITEM);
    selectItem.set(SELECT_ITEM_EXPR, columnRef);
    selectItem.set(SELECT_ITEM_ALIAS, name());

    return colName;
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
