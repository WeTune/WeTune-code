package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

class InputNodeImpl extends PlanNodeBase implements InputNode {
  private final Table table;
  private final ValueBag values;
  private final int uniqueId;

  private static final AtomicInteger NEXT_UNIQUE_ID = new AtomicInteger(0);

  private InputNodeImpl(Table table, ValueBag values, int uniqueId) {
    this.table = requireNonNull(table);
    this.values = requireNonNull(values);
    this.uniqueId = uniqueId;
  }

  InputNodeImpl(Table table, String alias) {
    this(
        table,
        ValueBag.mk(listMap(table.columns(), ColumnValue::new)),
        NEXT_UNIQUE_ID.getAndIncrement());
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
    final InputNode copy = new InputNodeImpl(table, values, uniqueId);
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
    } else {
      builder.append("@").append(uniqueId);
    }
    builder.append('}');
    return builder;
  }
}
