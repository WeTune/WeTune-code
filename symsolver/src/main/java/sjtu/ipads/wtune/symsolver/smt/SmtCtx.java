package sjtu.ipads.wtune.symsolver.smt;

import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.smt.impl.z3.Z3SmtCtx;

public interface SmtCtx {
  Proposition makeConst(boolean bool);

  Proposition makeEq(Value v0, Value v1);

  Proposition makeEq(Func f0, Func f1);

  Proposition makeNot(Proposition p);

  Proposition makeImplies(Proposition p0, Proposition p1);

  Proposition makeAnd(Proposition p0, Proposition p1);

  Proposition makeForAll(Value[] args, Proposition assertions);

  Proposition makeExists(Value[] args, Proposition assertions);

  Proposition makeTracker(String name);

  Func makeFunc(Sym t);

  Value makeTuple(String name);

  Value makeApply(Func func, Value... args);

  SmtSolver makeSolver();

  default Proposition makeTautology() {
    return makeConst(true);
  }

  default Value[] makeTuples(int n, String prefix) {
    if (n == 1) return new Value[] {makeTuple(prefix)};

    final Value[] tuples = new Value[n];
    for (int i = 0; i < n; i++) tuples[i] = makeTuple(prefix + i);
    return tuples;
  }

  default Proposition tupleFrom(Value tuple, TableSym table) {
    return (Proposition) table.apply(tuple);
  }

  default Proposition tuplesFrom(Value[] tuples, TableSym[] tables) {
    if (tuples.length != tables.length) throw new IllegalArgumentException();

    Proposition proposition = null;
    int i = 0;
    for (TableSym table : tables) proposition = tupleFrom(tuples[i++], table).and(proposition);

    return proposition == null ? makeTautology() : proposition;
  }

  static SmtCtx z3() {
    return Z3SmtCtx.build();
  }
}
