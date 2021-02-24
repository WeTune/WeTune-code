package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class NativePlanAttribute extends PlanAttributeBase {
  private final Column column;

  private NativePlanAttribute(String qualification, Column column) {
    super(qualification, column.name());
    this.column = column;
  }

  public static List<PlanAttribute> build(Table rel, String tableAlias) {
    final Collection<? extends Column> columns = rel.columns();
    return listMap(it -> new NativePlanAttribute(tableAlias, it), columns);
  }

  @Override
  public ASTNode expr() {
    return null;
  }

  @Override
  public String[] referenceName() {
    return null;
  }

  @Override
  public Column column(boolean recursive) {
    return column;
  }

  @Override
  public PlanAttribute reference(boolean recursive) {
    return this;
  }

  @Override
  public List<PlanAttribute> used() {
    return Collections.singletonList(this);
  }

  @Override
  public void setUsed(List<PlanAttribute> used) {}

  @Override
  public boolean refEquals(PlanAttribute other) {
    if (other instanceof NativePlanAttribute) return this == other;
    else if (other instanceof DerivedPlanAttribute) return this == other.reference(true);
    else return false;
  }

  @Override
  public String toString() {
    return column + " AS " + qualification() + "." + name();
  }
}
