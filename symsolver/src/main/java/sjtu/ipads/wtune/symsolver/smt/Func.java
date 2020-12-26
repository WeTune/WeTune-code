package sjtu.ipads.wtune.symsolver.smt;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.symsolver.smt.impl.FuncImpl;

public interface Func extends Value {
  static Func wrap(SmtCtx ctx, String name, int arity, Object underlying) {
    return FuncImpl.build(ctx, name, arity, underlying);
  }

  Proposition equalsTo(Func other);

  Value apply(Value... v);

  int arity();

  default Value apply(Iterable<Value> v) {
    return apply(Iterables.toArray(v, Value.class));
  }
}
