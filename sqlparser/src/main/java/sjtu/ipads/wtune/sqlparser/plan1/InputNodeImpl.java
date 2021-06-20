package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.schema.Table;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

class InputNodeImpl extends PlanNodeBase implements InputNode {
  private final Table table;
  private final ValueBag values;

  private InputNodeImpl(Table table, ValueBag values) {
    this.table = requireNonNull(table);
    this.values = requireNonNull(values);
  }

  InputNodeImpl(Table table, String alias) {
    this(table, new ValueBagImpl(listMap(ColumnValue::new, table.columns())));
    values.setQualification(simpleName(alias));
  }

  @Override
  public ValueBag values() {
    return values;
  }

  @Override
  public RefBag refs() {
    return RefBag.empty();
  }

  @Override
  public Table table() {
    return table;
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    final InputNode copy = new InputNodeImpl(table, values);
    copy.setContext(ctx);
    ctx.registerValues(copy, values());
    return copy;
  }

  @Override
  public String toString() {
    return "Input{%s}".formatted(table);
  }
}
