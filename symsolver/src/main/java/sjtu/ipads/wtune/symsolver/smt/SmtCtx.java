package sjtu.ipads.wtune.symsolver.smt;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.smt.impl.z3.Z3SmtCtx;

import java.util.Collection;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.asArray;

public interface SmtCtx {
  Proposition makeEq(Value v0, Value v1);

  Proposition makeEq(Func f0, Func f1);

  Proposition makeImplies(Proposition p0, Proposition p1);

  Proposition makeAnd(Proposition p0, Proposition p1);

  Proposition makeBool(String name);

  Func makeFunc(TableSym t);

  Func makeFunc(PickSym t);

  Func makeCombine(int n);

  Value makeApply(Func func, Value... args);

  Value makeTuple(String name);

  Proposition makeForAll(Value[] args, Proposition assertions);

  Proposition makeExists(Value[] args, Proposition assertions);

  SmtSolver makeSolver();

  SmtSolver makeSolver(Map<String, Object> option);

  Collection<Func> declaredFuncs();

  default Value[] makeTuples(int n, String prefix) {
    final Value[] tuples = new Value[n];
    for (int i = 0; i < n; i++) tuples[i] = makeTuple(prefix + i);
    return tuples;
  }

  default Proposition makeForAll(Value arg, Proposition assertions) {
    return makeForAll(asArray(arg), assertions);
  }

  default Proposition makeExists(Value arg, Proposition assertions) {
    return makeExists(asArray(arg), assertions);
  }

  default Proposition tupleFrom(Value tuple, TableSym table) {
    return (Proposition) makeFunc(table).apply(tuple);
  }

  default Proposition tuplesFrom(Value[] tuples, Collection<TableSym> tables) {
    if (tuples.length != tables.size()) throw new IllegalArgumentException();

    Proposition proposition = null;
    int i = 0;
    for (TableSym table : tables) proposition = tupleFrom(tuples[i], table).and(proposition);

    return proposition;
  }

  default Value combine(Value... values) {
    return makeCombine(values.length).apply(values);
  }

  default Value pick(PickSym pick, Value... values) {
    return makeFunc(pick).apply(combine(values));
  }

  static SmtCtx z3() {
    return Z3SmtCtx.build();
  }
}
