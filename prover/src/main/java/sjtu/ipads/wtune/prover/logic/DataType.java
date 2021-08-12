package sjtu.ipads.wtune.prover.logic;

interface DataType extends LogicObj {
  Func constructor();

  Func accessor(String name);

  void setConstructor(Func constructor);

  void setAccessors(Func[] accessor);

  static DataType mk(LogicCtx ctx, Object underlying) {
    return new DataTypeImpl(ctx, underlying, null, null);
  }

  static DataType mk(LogicCtx ctx, Object underlying, Func constructor, Func[] accessors) {
    return new DataTypeImpl(ctx, underlying, constructor, accessors);
  }
}
