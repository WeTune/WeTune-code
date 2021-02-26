package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.internal.DerivedPlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.internal.NativePlanAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

/**
 * This class describes the <b>output</b> attributes of an plan node.<br>
 * <br>
 *
 * <p>Example
 *
 * <pre>SELECT * FROM (SELECT a.i AS j FROM a) AS b</pre>
 *
 * <ul>
 *   <li>The output attribute of the table source `a` is [`a.i`]
 *   <li>The output attribute of the nested query is [`b.j`] (instead of `a.i`)
 *   <li>The output attribute of the outer query is [`b.j`]
 * </ul>
 */
public interface PlanAttribute {
  String qualification();

  String name();

  ASTNode expr();

  Column column();

  PlanNode origin();

  void setOrigin(PlanNode origin);

  PlanAttribute copy();

  default boolean isReferencedBy(String qualification, String alias) {
    return (qualification == null || qualification.equals(qualification())) && alias.equals(name());
  }

  default PlanAttribute reference(boolean recursive) {
    final PlanNode origin = origin();
    if (origin instanceof InputNode) return this;

    final ASTNode colName = expr().get(COLUMN_REF_COLUMN);
    if (colName == null) return null;

    final String qualification = colName.get(COLUMN_NAME_TABLE);
    final String name = colName.get(COLUMN_NAME_COLUMN);

    for (PlanNode predecessor : origin.predecessors()) {
      final PlanAttribute attr = predecessor.resolveAttribute(qualification, name);
      if (attr != null)
        if (recursive) return attr.reference(true);
        else return attr;
    }

    return null;
  }

  default ASTNode toSelectItem() {
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    final ASTNode expr;
    if (expr() != null) expr = expr().deepCopy();
    else {
      final Column column = column();
      assert column != null;

      final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
      colName.set(COLUMN_NAME_TABLE, column.tableName());
      colName.set(COLUMN_NAME_COLUMN, column.name());

      expr = ASTNode.expr(COLUMN_REF);
      expr.set(COLUMN_REF_COLUMN, colName);
    }

    item.set(SELECT_ITEM_ALIAS, name());
    item.set(SELECT_ITEM_EXPR, expr);
    return item;
  }

  /** Returns a ColumnRef referencing this attribute. */
  default ASTNode toColumnRef() {
    if (name() == null) throw new IllegalStateException("anonymous attribute cannot be reference");

    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, qualification());
    colName.set(COLUMN_NAME_COLUMN, name());

    final ASTNode ref = ASTNode.expr(COLUMN_REF);
    ref.set(COLUMN_REF_COLUMN, colName);

    return ref;
  }

  static PlanAttribute fromColumn(String tableAlias, Column c) {
    return NativePlanAttribute.fromColumn(tableAlias, c);
  }

  static PlanAttribute fromExpr(String qualification, String name, ASTNode expr) {
    return DerivedPlanAttribute.fromExpr(qualification, name, expr);
  }
}
