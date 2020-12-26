package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

public interface Query {
  String name();

  Query setName(String name);

  TableSym[] tables();

  PickSym[] picks();

  Value output(SmtCtx ctx, Value[] tuples);

  Proposition condition(SmtCtx ctx, Value[] tuples);
}
