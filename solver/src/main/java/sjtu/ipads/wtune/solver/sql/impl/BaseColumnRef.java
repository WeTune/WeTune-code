package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

public abstract class BaseColumnRef implements ColumnRef {
  private String alias;
  private final DataType dataType;
  private final boolean notNull;

  private AlgNode owner;

  protected BaseColumnRef(String alias, DataType dataType, boolean notNull) {
    this.alias = alias;
    this.dataType = dataType;
    this.notNull = notNull;
  }

  @Override
  public String alias() {
    return alias;
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
  public AlgNode owner() {
    return owner;
  }

  @Override
  public ColumnRef setAlias(String alias) {
    this.alias = alias;
    return this;
  }

  @Override
  public ColumnRef setOwner(AlgNode owner) {
    this.owner = owner;
    return this;
  }

  @Override
  public String toString() {
    return alias;
  }
}
