package sjtu.ipads.wtune.prover.logic;

public interface LogicCtx {
  DataType mkIntType();

  DataType mkBoolType();

  DataType mkTupleType(String name, String[] members);

  Value mkConst(int i);

  Value mkIte(Proposition cond, Value v0, Value v1);

  Value mkVal(String name, DataType dataType);

  Value mkIntVal(String name);

  Value mkProduct(Value... vs);

  Value mkSum(Value... vs);

  Value mkApply(Func func, Value... args);

  Proposition mkDisjunction(Proposition... ps);

  Proposition mkConjunction(Proposition... ps);

  Proposition mkBoolVal(String name);

  Proposition mkForall(Value[] args, Proposition assertions);

  Proposition mkEq(Value v0, Value v1);

  Proposition mkGt(Value v0, Value v1);

  Proposition mkGe(Value v0, Value v1);

  Proposition mkNot(Proposition p);

  Proposition mkImplies(Proposition premise, Proposition consequence);

  Proposition mkExists(Value[] args, Proposition assertions);

  Func mkFunc(String name, DataType retType, DataType... argTypes);

  LogicSolver mkSolver();

  void close();

  static LogicCtx z3() {
    return Z3LogicCtx.mk();
  }

  default Proposition mkForall(Value arg, Proposition assertions) {
    return mkForall(new Value[] {arg}, assertions);
  }

  default Proposition mkExists(Value arg, Proposition assertions) {
    return mkExists(new Value[] {arg}, assertions);
  }
}
