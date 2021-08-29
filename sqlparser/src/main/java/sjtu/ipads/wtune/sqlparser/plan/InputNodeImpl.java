package sjtu.ipads.wtune.sqlparser.plan;

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
    this(table, ValueBag.mk(listMap(table.columns(), ColumnValue::new)));
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
  public PlanNode copy(PlanContext ctx) {
    final InputNode copy = new InputNodeImpl(table, values);
    copy.setContext(ctx);
    ctx.registerValues(copy, values());
    return copy;
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder, boolean compact) {
    builder.append("Input{").append(table.name());
    if (!compact) {
      final String qualification = values.get(0).qualification();
      if (qualification != null) builder.append(" AS ").append(qualification);
    }
    builder.append('}');
    return builder;
  }
}
