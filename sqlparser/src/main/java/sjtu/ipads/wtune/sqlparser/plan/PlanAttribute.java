package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.internal.DerivedPlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.internal.NativePlanAttribute;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRefs;

public interface PlanAttribute {
  String qualification();

  String name();

  ASTNode expr();

  String[] referenceName();

  PlanAttribute reference(boolean recursive);

  Column column(boolean recursive);

  List<PlanAttribute> used();

  void setUsed(List<PlanAttribute> used);

  boolean refEquals(PlanAttribute other);

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
    final PlanAttribute ref = column != null ? this : reference(false);
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_ALIAS, name());
    item.set(SELECT_ITEM_EXPR, ref == null ? expr().deepCopy() : ref.toColumnRef());

    final List<PlanAttribute> usedAttrs = used();
    if (!isEmpty(usedAttrs)) {
      final List<ASTNode> colRefs = collectColumnRefs(item);
      for (int i = 0, bound = colRefs.size(); i < bound; i++) {
        final PlanAttribute usedAttr = usedAttrs.get(i);
        if (usedAttr != null) colRefs.get(i).update(usedAttr.toColumnRef());
      }
    }

    return item;
  }

  static List<PlanAttribute> fromInput(Table table, String tableAlias) {
    return NativePlanAttribute.build(table, tableAlias);
  }

  static List<PlanAttribute> fromAttrs(List<Attribute> attrs, String qualification) {
    return DerivedPlanAttribute.build(attrs, qualification);
  }
}
