package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class NativePlanAttribute extends PlanAttributeBase {
  private final Column column;

  private NativePlanAttribute(String qualification, Column column) {
    super(qualification, column.name());
    this.column = column;
  }

  public static PlanAttribute fromColumn(String tableAlias, Column c) {
    return new NativePlanAttribute(tableAlias, c);
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
  public Column column() {
    return column;
  }

  @Override
  public String toString() {
    return column + " AS " + qualification() + "." + name();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NativePlanAttribute that = (NativePlanAttribute) o;
    return Objects.equals(column, that.column);
  }

  @Override
  public int hashCode() {
    return Objects.hash(column);
  }

  @Override
  public PlanAttribute copy() {
    return new NativePlanAttribute(qualification(), column());
  }
}
