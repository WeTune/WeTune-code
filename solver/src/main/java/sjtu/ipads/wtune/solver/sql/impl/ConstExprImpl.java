package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.expr.ConstExpr;

public class ConstExprImpl implements ConstExpr {
  private final DataType dataType;
  private final Object value;
  private final boolean notNull;

  protected ConstExprImpl(DataType dataType, Object value, boolean notNull) {
    this.dataType = dataType;
    this.value = value;
    this.notNull = notNull;
  }

  public static ConstExpr create(Object value) {
    return new ConstExprImpl(inferDataType(value), value, value != null);
  }

  private static DataType inferDataType(Object value) {
    return null; // TODO
  }

  @Override
  public DataType dataType() {
    return dataType;
  }

  @Override
  public boolean notNull() {
    return notNull;
  }

  @Override
  public Object value() {
    return value;
  }

  @Override
  public String toString() {
    return value == null ? "NULL" : value.toString();
  }
}
