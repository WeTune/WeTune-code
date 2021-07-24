package sjtu.ipads.wtune.prover.logic;

import com.microsoft.z3.*;

import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.generate;

class Z3LogicCtx implements LogicCtx {
  static {
    Global.setParameter("smt.auto_config", "false");
    Global.setParameter("smt.random_seed", "11235");
    Global.setParameter("smt.qi.quick_checker", "2");
    Global.setParameter("smt.qi.max_multi_patterns", "1024");
    Global.setParameter("smt.mbqi.max_iterations", "3");
    //    Global.setParameter("timeout", System.getProperty("wetune.smt_timeout", "100"));
    Global.setParameter("combined_solver.solver2_unknown", "2");
    Global.setParameter("pp.max_depth", "100");
  }

  private final Context z3;

  private Z3LogicCtx() {
    z3 = new Context();
  }

  public static LogicCtx mk() {
    return new Z3LogicCtx();
  }

  @Override
  public DataType mkIntType() {
    return DataType.mk(this, z3.getIntSort());
  }

  @Override
  public DataType mkBoolType() {
    return DataType.mk(this, z3.getBoolSort());
  }

  @Override
  public DataType mkTupleType(String name, String[] members) {
    final Sort[] sorts = generate(members.length, it -> z3.getIntSort(), Sort.class);

    final Constructor cons = z3.mkConstructor("mk", "is_" + name, members, sorts, null);
    final DatatypeSort d = z3.mkDatatypeSort(name, new Constructor[] {cons});

    return wrap(d);
  }

  @Override
  public Value mkVal(String name, DataType dataType) {
    return wrap(z3.mkConst(name, unwrap(dataType)));
  }

  @Override
  public Value mkIntVal(String name) {
    return wrap(z3.mkIntConst(name));
  }

  @Override
  public Value mkConst(int i) {
    return wrap(z3.mkInt(i));
  }

  @Override
  public Value mkIte(Proposition cond, Value v0, Value v1) {
    return wrap(z3.mkITE(unwrap(cond), unwrap(v0), unwrap(v1)));
  }

  @Override
  public Value mkApply(Func func, Value... args) {
    return wrap(unwrap(func).apply(unwrap(args)));
  }

  @Override
  public Value mkProduct(Value... vs) {
    if (vs.length == 1) return vs[0];

    final ArithExpr[] operands = arrayMap(vs, it -> (ArithExpr) unwrap(it), ArithExpr.class);
    return wrap(z3.mkMul(operands));
  }

  @Override
  public Value mkSum(Value... vs) {
    if (vs.length == 1) return vs[0];
    final ArithExpr[] operands = arrayMap(vs, it -> (ArithExpr) unwrap(it), ArithExpr.class);
    return wrap(z3.mkAdd(operands));
  }

  @Override
  public Proposition mkDisjunction(Proposition... ps) {
    if (ps.length == 1) return ps[0];
    final BoolExpr[] operands = arrayMap(ps, Z3LogicCtx::unwrap, BoolExpr.class);
    return wrap(z3.mkOr(operands));
  }

  @Override
  public Proposition mkConjunction(Proposition... ps) {
    if (ps.length == 1) return ps[0];
    final BoolExpr[] operands = arrayMap(ps, Z3LogicCtx::unwrap, BoolExpr.class);
    return wrap(z3.mkAnd(operands));
  }

  @Override
  public Proposition mkBoolVal(String name) {
    return wrap(z3.mkBoolConst(name));
  }

  @Override
  public Proposition mkEq(Value v0, Value v1) {
    return wrap(z3.mkEq(unwrap(v0), unwrap(v1)));
  }

  @Override
  public Proposition mkGt(Value v0, Value v1) {
    return wrap(z3.mkGt((ArithExpr) unwrap(v0), (ArithExpr) unwrap(v1)));
  }

  @Override
  public Proposition mkNot(Proposition p) {
    return wrap(z3.mkNot(unwrap(p)));
  }

  @Override
  public Proposition mkImplies(Proposition premise, Proposition consequence) {
    return wrap(z3.mkImplies(unwrap(premise), unwrap(consequence)));
  }

  @Override
  public Proposition mkForall(Value[] args, Proposition assertions) {
    return wrap(z3.mkForall(unwrap(args), unwrap(assertions), 1, null, null, null, null));
  }

  @Override
  public Proposition mkExists(Value[] args, Proposition assertions) {
    return wrap(z3.mkExists(unwrap(args), unwrap(assertions), 1, null, null, null, null));
  }

  @Override
  public LogicSolver mkSolver() {
    return LogicSolver.z3(z3.mkSolver());
  }

  @Override
  public Func mkFunc(String name, DataType retType, DataType... argTypes) {
    return wrap(z3.mkFuncDecl(name, unwrap(argTypes), unwrap(retType)));
  }

  @Override
  public void close() {
    z3.close();
  }

  private Value wrap(Expr expr) {
    if (expr instanceof BoolExpr) return Proposition.wrap(this, expr);
    else return Value.wrap(this, expr);
  }

  private Proposition wrap(BoolExpr expr) {
    return Proposition.wrap(this, expr);
  }

  private Func wrap(FuncDecl decl) {
    return Func.wrap(
        this,
        decl,
        decl.getName().toString(),
        arrayMap(decl.getDomain(), this::wrap, DataType.class));
  }

  private DataType wrap(Sort sort) {
    if (sort instanceof DatatypeSort) {
      final DatatypeSort dt = (DatatypeSort) sort;
      return DataType.mk(
          this,
          sort,
          wrap(dt.getConstructors()[0]),
          arrayMap(
              dt.getAccessors()[0],
              it -> Func.wrap(this, it, it.getName().toString(), null),
              Func.class));
    } else {
      return DataType.mk(this, sort);
    }
  }

  static Sort unwrap(DataType t) {
    return t.unwrap(Sort.class);
  }

  static Sort[] unwrap(DataType... ts) {
    return arrayMap(ts, Z3LogicCtx::unwrap, Sort.class);
  }

  static Expr unwrap(Value v) {
    return v.unwrap(Expr.class);
  }

  static Expr[] unwrap(Value... vs) {
    return arrayMap(vs, Z3LogicCtx::unwrap, Expr.class);
  }

  static BoolExpr unwrap(Proposition p) {
    return p.unwrap(BoolExpr.class);
  }

  static FuncDecl unwrap(Func f) {
    return f.unwrap(FuncDecl.class);
  }
}
