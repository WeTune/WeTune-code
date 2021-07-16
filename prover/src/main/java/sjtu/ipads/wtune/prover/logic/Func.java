package sjtu.ipads.wtune.prover.logic;

import com.google.common.collect.Iterables;

interface Func extends Value {
  static Func wrap(LogicCtx ctx, Object underlying, String name, DataType[] paramTypes) {
    return new FuncImpl(ctx, underlying, name, paramTypes);
  }

  Value apply(Value... v);

  DataType[] paramTypes();

  String name();

  default Value apply(Iterable<Value> v) {
    return apply(Iterables.toArray(v, Value.class));
  }
}
