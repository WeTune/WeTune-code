package sjtu.ipads.wtune.symsolver.smt.impl.z3;

import com.microsoft.z3.*;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.smt.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.repeat;

public class Z3SmtCtx implements SmtCtx {
  static {
    Global.setParameter("timeout", "30");
    Global.setParameter("pp.max_depth", "100");
  }

  private final Context z3;
  private final Map<String, Func> funcCache;

  private Z3SmtCtx() {
    z3 = new Context();
    funcCache = new HashMap<>();
  }

  public static SmtCtx build() {
    return new Z3SmtCtx();
  }

  static Expr unwrap(Value v) {
    return v.unwrap(Expr.class);
  }

  static Expr[] unwrap(Value... vs) {
    return arrayMap(Z3SmtCtx::unwrap, Expr.class, vs);
  }

  static BoolExpr unwrap(Proposition p) {
    return p.unwrap(BoolExpr.class);
  }

  static FuncDecl unwrap(Func f) {
    return f.unwrap(FuncDecl.class);
  }

  @Override
  public Proposition makeEq(Value v0, Value v1) {
    return wrap(z3.mkEq(unwrap(v0), unwrap(v1)));
  }

  @Override
  public Proposition makeEq(Func f0, Func f1) {
    final FuncDecl fd0 = unwrap(f0), fd1 = unwrap(f1);
    if (fd0.getArity() != fd1.getArity()) throw new IllegalArgumentException();

    final Value[] args = makeTuples(fd0.getArity(), "x");
    return makeForAll(args, f0.apply(args).equalsTo(f1.apply(args)));
  }

  @Override
  public Proposition makeImplies(Proposition p0, Proposition p1) {
    return wrap(z3.mkImplies(unwrap(p0), unwrap(p1)));
  }

  @Override
  public Proposition makeAnd(Proposition p0, Proposition p1) {
    return wrap(z3.mkAnd(unwrap(p0), unwrap(p1)));
  }

  @Override
  public Proposition makeNot(Proposition p) {
    return wrap(z3.mkNot(unwrap(p)));
  }

  @Override
  public Proposition makeBool(String name) {
    return wrap(z3.mkBoolConst(name));
  }

  @Override
  public Func makeFunc(TableSym t) {
    return makeFunc0("t" + t.index(), z3.getBoolSort(), tupleSort());
  }

  @Override
  public Func makeFunc(PickSym p) {
    return makeFunc0("p" + p.index(), tupleSort(), tupleSort());
  }

  @Override
  public Value makeTuple(String name) {
    return wrap(z3.mkConst(name, tupleSort()));
  }

  @Override
  public Value makeApply(Func func, Value... args) {
    if (func.arity() != args.length) throw new IllegalArgumentException("mismatched # of args");

    if (func.name().startsWith("combine") && func.arity() == 1) return args[0];
    return wrap(unwrap(func).apply(unwrap(args)));
  }

  @Override
  public Value makeCombine(Value... values) {
    switch (values.length) {
      case 0:
        return wrap(z3.mkInt(1));
      case 1:
        return values[0];
      default:
        return makeCombine0(values.length).apply(values);
    }
  }

  @Override
  public Proposition makeForAll(Value[] args, Proposition assertions) {
    return wrap(z3.mkForall(unwrap(args), unwrap(assertions), 1, null, null, null, null));
  }

  @Override
  public Proposition makeExists(Value[] args, Proposition assertions) {
    return wrap(z3.mkExists(unwrap(args), unwrap(assertions), 1, null, null, null, null));
  }

  @Override
  public SmtSolver makeSolver() {
    final Solver solver = z3.mkSolver();
    final Params params = z3.mkParams();

    //    params.add("timeout", 100);
    //    solver.setParameters(params);

    return SmtSolver.z3(solver);
  }

  @Override
  public Collection<Func> declaredFuncs() {
    return funcCache.values();
  }

  private Sort tupleSort() {
    return z3.getIntSort();
  }

  private Func makeCombine0(int n) {
    return makeFunc0("combine" + n, tupleSort(), repeat(tupleSort(), n));
  }

  private Func makeFunc0(String name, Sort retSort, Sort... argSorts) {
    return funcCache.computeIfAbsent(name, n -> wrap(z3.mkFuncDecl(n, argSorts, retSort)));
  }

  private Value wrap(Expr expr) {
    if (expr instanceof BoolExpr) return Proposition.wrap(this, expr);
    else return Value.wrap(this, expr);
  }

  private Proposition wrap(BoolExpr expr) {
    return Proposition.wrap(this, expr);
  }

  private Func wrap(FuncDecl decl) {
    return Func.wrap(this, decl.getName().toString(), decl.getArity(), decl);
  }
}
