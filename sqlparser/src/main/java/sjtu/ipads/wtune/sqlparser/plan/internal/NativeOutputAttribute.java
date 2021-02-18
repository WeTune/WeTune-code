package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class NativeOutputAttribute extends OutputAttributeBase {
  private final Column column;

  private NativeOutputAttribute(PlanNode owner, String qualification, Column column) {
    super(owner, qualification, column.name());
    this.column = column;
  }

  public static List<OutputAttribute> build(PlanNode node, Table rel, String tableAlias) {
    final Collection<? extends Column> columns = rel.columns();
    return listMap(it -> new NativeOutputAttribute(node, tableAlias, it), columns);
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
  public OutputAttribute reference(boolean recursive) {
    return this;
  }

  @Override
  public List<OutputAttribute> used() {
    return Collections.singletonList(this);
  }

  @Override
  public void setReference(OutputAttribute reference) {}

  @Override
  public void setUsed(List<OutputAttribute> used) {}

  @Override
  public boolean refEquals(OutputAttribute other) {
    if (other instanceof NativeOutputAttribute) return this == other;
    else if (other instanceof DerivedOutputAttribute) return this == other.reference(true);
    else return false;
  }
}
