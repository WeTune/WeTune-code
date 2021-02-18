package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.internal.DerivedOutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.internal.NativeOutputAttribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;

public interface OutputAttribute {
  PlanNode origin();

  String qualification();

  String name();

  ASTNode expr();

  String[] referenceName();

  OutputAttribute reference(boolean recursive);

  Column column(boolean recursive);

  List<OutputAttribute> used();

  void setReference(OutputAttribute attribute);

  void setUsed(List<OutputAttribute> used);

  boolean refEquals(OutputAttribute other);

  default ASTNode toColumnRef() {
    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, qualification());
    colName.set(COLUMN_NAME_COLUMN, name());

    final ASTNode ref = ASTNode.expr(ExprKind.COLUMN_REF);
    ref.set(COLUMN_REF_COLUMN, colName);

    return ref;
  }

  default ASTNode toSelectItem() {
    final Column column = column(false);
    final OutputAttribute ref = column != null ? this : reference(false);
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_ALIAS, name());
    item.set(SELECT_ITEM_EXPR, ref == null ? expr().copy() : ref.toColumnRef());
    return item;
  }

  static List<OutputAttribute> fromInput(PlanNode node, Table table, String tableAlias) {
    return NativeOutputAttribute.build(node, table, tableAlias);
  }

  static List<OutputAttribute> fromProj(PlanNode node, Relation rel) {
    return DerivedOutputAttribute.build(node, rel);
  }
}
