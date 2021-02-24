package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

public class DerivedPlanAttribute extends PlanAttributeBase {
  private final ASTNode expr;
  private final String[] referenceName;
  private List<PlanAttribute> used;

  public DerivedPlanAttribute(
      String qualification, String name, ASTNode expr, String[] referenceName) {
    super(qualification, name);
    this.expr = expr;
    this.referenceName = referenceName;
  }

  public static PlanAttribute fromExpr(String qualification, String name, ASTNode expr) {
    final String[] refName;
    if (COLUMN_REF.isInstance(expr)) {
      if (qualification == null) qualification = expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_TABLE);

      final ASTNode colName = expr.get(COLUMN_REF_COLUMN);
      refName = new String[] {colName.get(COLUMN_NAME_TABLE), colName.get(COLUMN_NAME_COLUMN)};
    } else refName = null;

    return new DerivedPlanAttribute(qualification, name, expr, refName);
  }

  public static List<PlanAttribute> build(List<Attribute> attrs, String qualification) {
    final List<PlanAttribute> outAttrs = new ArrayList<>(attrs.size());
    for (Attribute attr : attrs) {
      final ASTNode expr =
          attr instanceof DerivedAttribute
              ? attr.selectItem().get(SELECT_ITEM_EXPR)
              : attr.toColumnRef();

      if (qualification == null && COLUMN_REF.isInstance(expr))
        qualification = expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_TABLE);

      final Attribute ref = attr.reference(false);
      final String[] referenceName =
          ref != null ? new String[] {ref.owner().alias(), ref.name()} : null;

      outAttrs.add(new DerivedPlanAttribute(qualification, attr.name(), expr, referenceName));
    }
    return outAttrs;
  }

  @Override
  public String[] referenceName() {
    return referenceName;
  }

  @Override
  public ASTNode expr() {
    return expr;
  }

  @Override
  public Column column(boolean recursive) {
    final PlanAttribute ref = reference(true);
    return ref == null ? null : ref.column(true);
  }

  @Override
  public PlanAttribute reference(boolean recursive) {
    final PlanAttribute reference =
        isEmpty(used) || !COLUMN_REF.isInstance(expr) ? null : used.get(0);

    if (!recursive || reference == null) return reference;
    return reference.reference(true);
  }

  @Override
  public List<PlanAttribute> used() {
    return used;
  }

  @Override
  public void setUsed(List<PlanAttribute> used) {
    this.used = used;
  }

  @Override
  public boolean refEquals(PlanAttribute other) {
    if (other instanceof NativePlanAttribute) return reference(true) == other;
    else if (other instanceof DerivedPlanAttribute) return reference(true) == other.reference(true);
    else return false;
  }
}
