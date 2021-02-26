package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

public class DerivedPlanAttribute extends PlanAttributeBase {
  private final ASTNode expr;

  public DerivedPlanAttribute(String qualification, String name, ASTNode expr) {
    super(qualification, name);
    this.expr = expr;
  }

  public static PlanAttribute fromExpr(String qualification, String name, ASTNode expr) {
    if (qualification == null && COLUMN_REF.isInstance(expr))
      qualification = expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_TABLE);

    return new DerivedPlanAttribute(qualification, name, expr);
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

      outAttrs.add(new DerivedPlanAttribute(qualification, attr.name(), expr));
    }
    return outAttrs;
  }

  @Override
  public ASTNode expr() {
    return expr;
  }

  @Override
  public Column column() {
    final PlanAttribute ref = reference(true);
    return ref != null ? ref.column() : null;
  }

  @Override
  public boolean isReferencedBy(String qualification, String alias) {
    if (super.isReferencedBy(qualification, alias)) return true;

    final PlanAttribute ref = reference(false);
    return ref != null && ref.isReferencedBy(qualification, alias);
  }

  @Override
  public PlanAttribute copy() {
    return new DerivedPlanAttribute(qualification(), name(), expr());
  }
}
