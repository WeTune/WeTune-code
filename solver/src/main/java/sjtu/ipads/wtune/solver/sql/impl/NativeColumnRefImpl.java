package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Column;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.NativeColumnRef;

import java.util.Objects;

public class NativeColumnRefImpl extends BaseColumnRef implements NativeColumnRef {
  private final TableNode source;
  private final Column column;

  private NativeColumnRefImpl(
      String alias, DataType dataType, boolean notNull, TableNode source, Column column) {
    super(alias, dataType, notNull);
    this.source = source;
    this.column = column;
  }

  public static NativeColumnRef create(TableNode source, Column column) {
    return new NativeColumnRefImpl(
            column.name(), column.dataType(), column.notNull(), source, column)
        .setOwner(source);
  }

  @Override
  public TableNode source() {
    return source;
  }

  @Override
  public Column column() {
    return column;
  }

  @Override
  public NativeColumnRef setOwner(AlgNode owner) {
    super.setOwner(owner);
    return this;
  }

  @Override
  public NativeColumnRef copy() {
    return new NativeColumnRefImpl(alias(), dataType(), notNull(), source(), column())
        .setOwner(owner());
  }

  @Override
  public String toString() {
    return column.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NativeColumnRefImpl that = (NativeColumnRefImpl) o;
    return Objects.equals(column, that.column);
  }

  @Override
  public int hashCode() {
    return Objects.hash(column);
  }
}
