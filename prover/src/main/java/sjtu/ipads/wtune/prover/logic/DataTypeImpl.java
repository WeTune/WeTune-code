package sjtu.ipads.wtune.prover.logic;

import static sjtu.ipads.wtune.common.utils.FuncUtils.find;

import java.util.Objects;

class DataTypeImpl extends LogicObjImpl implements DataType {
  private Func constructor;
  private Func[] accessors;

  DataTypeImpl() {}

  DataTypeImpl(LogicCtx ctx, Object obj, Func constructor, Func[] accessors) {
    super(ctx, obj);
    this.constructor = constructor;
    this.accessors = accessors;
  }

  @Override
  public Func constructor() {
    return constructor;
  }

  @Override
  public Func accessor(String name) {
    if (accessors == null) return null;
    return find(accessors, it -> it.name().equals(name));
  }

  @Override
  public void setConstructor(Func constructor) {
    this.constructor = constructor;
  }

  @Override
  public void setAccessors(Func[] accessor) {
    this.accessors = accessor;
  }

  @Override
  public String toString() {
    return underlying().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DataType)) return false;
    final DataType dataType = (DataType) o;
    return Objects.equals(underlying(), dataType.underlying());
  }

  @Override
  public int hashCode() {
    return underlying().hashCode();
  }
}
