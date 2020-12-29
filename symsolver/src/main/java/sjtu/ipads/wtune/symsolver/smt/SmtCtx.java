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

  Proposition makeNot(Proposition p);

  Proposition makeImplies(Proposition p0, Proposition p1);

  Proposition makeAnd(Proposition p0, Proposition p1);

  Proposition makeBool(String name);

  Func makeFunc(TableSym t);

  Func makeFunc(PickSym t);

  Value makeTuple(String name);

  Value makeApply(Func func, Value... args);

  Value makeCombine(Value... values);

  Proposition makeForAll(Value[] args, Proposition assertions);

  Proposition makeExists(Value[] args, Proposition assertions);

  SmtSolver makeSolver();

  Collection<Func> declaredFuncs();

  default Value[] makeTuples(int n, String prefix) {
    final Value[] tuples = new Value[n];
    for (int i = 0; i < n; i++) tuples[i] = makeTuple(prefix + i);
    return tuples;
  }

  default Proposition makeEqs(Value... vs) {
    Proposition p = Proposition.tautology();
    if (vs.length == 1) return p;

    final Value pivot = vs[0];
    for (int i = 1; i < vs.length; i++) p = p.and(makeEq(pivot, vs[i]));
    return p;
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

    Proposition proposition = Proposition.tautology();
    int i = 0;
    for (TableSym table : tables) proposition = tupleFrom(tuples[i++], table).and(proposition);

    return proposition;
  }

  default Value pick(PickSym pick, Value... values) {
    return makeFunc(pick).apply(makeCombine(values));
  }

  static SmtCtx z3() {
    return Z3SmtCtx.build();
  }
}
