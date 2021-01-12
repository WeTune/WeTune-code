package sjtu.ipads.wtune.symsolver.logic.impl.z3;

import com.microsoft.z3.*;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.*;

import java.util.HashMap;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.*;

public class Z3LogicCtx implements LogicCtx {
  static {
    final String timeout = System.getProperty("smt_timeout", "10");
    Global.setParameter("timeout", timeout);
    Global.setParameter("pp.max_depth", "100");
  }

  private final Context z3;
  private final Map<String, Func> funcCache;

  private Z3LogicCtx() {
    z3 = new Context();
    funcCache = new HashMap<>();
  }

  public static LogicCtx build() {
    return new Z3LogicCtx();
  }

  static Expr unwrap(Value v) {
    return v.unwrap(Expr.class);
  }

  static Expr[] unwrap(Value... vs) {
    return arrayMap(Z3LogicCtx::unwrap, Expr.class, vs);
  }

  static BoolExpr unwrap(Proposition p) {
    return p.unwrap(BoolExpr.class);
  }

  static BoolExpr[] unwrap(Proposition... ps) {
    return arrayMap(Z3LogicCtx::unwrap, BoolExpr.class, ps);
  }

  static FuncDecl unwrap(Func f) {
    return f.unwrap(FuncDecl.class);
  }

  @Override
  public Proposition makeConst(boolean bool) {
    return wrap(z3.mkBool(bool));
  }

  @Override
  public Proposition makeEq(Value v0, Value v1) {
    if (v0 instanceof Func && v1 instanceof Func) return makeEq((Func) v0, (Func) v1);
    else return wrap(z3.mkEq(unwrap(v0), unwrap(v1)));
  }

  @Override
  public Proposition makeEq(Func f0, Func f1) {
    final FuncDecl fd0 = unwrap(f0), fd1 = unwrap(f1);

    final Value arg = makeTuple("x");
    final Value ret0 = f0.apply(repeat(arg, fd0.getArity()));
    final Value ret1 = f1.apply(repeat(arg, fd1.getArity()));

    return makeForAll(asArray(arg), ret0.equalsTo(ret1));
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
  public Proposition makeOr(Proposition p0, Proposition p1) {
    return wrap(z3.mkOr(unwrap(p0), unwrap(p1)));
  }

  @Override
  public Proposition makeNot(Proposition p) {
    return wrap(z3.mkNot(unwrap(p)));
  }

  @Override
  public Proposition makeTracker(String name) {
    return wrap(z3.mkBoolConst(name));
  }

  @Override
  public Func makeFunc(Sym t) {
    if (t instanceof TableSym) return makeTableFunc((TableSym) t);
    if (t instanceof PickSym) return makePickFunc((PickSym) t);
    if (t instanceof PredicateSym) return makePredicateFunc((PredicateSym) t);
    throw new IllegalArgumentException("unknown symbol " + t);
  }

  @Override
  public Value makeTuple(String name) {
    return wrap(z3.mkConst(name, tupleSort()));
  }

  @Override
  public Value makeApply(Func func, Value... args) {
    if (func.arity() != args.length) throw new IllegalArgumentException("mismatched # of args");
    return wrap(unwrap(func).apply(unwrap(args)));
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
    return SmtSolver.z3(z3.mkSolver());
  }

  @Override
  public void close() {
    z3.close();
  }

  private Sort tupleSort() {
    return z3.getIntSort();
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

  private Func makeTableFunc(TableSym t) {
    return makeFunc0(tableFuncName(t), z3.getBoolSort(), tupleSort());
  }

  private Func makePickFunc(PickSym p) {
    return makeFunc0(pickFuncName(p), tupleSort(), repeat(tupleSort(), p.visibleSources().length));
  }

  private Func makePredicateFunc(PredicateSym p) {
    return makeFunc0(predicateFuncName(p), z3.getBoolSort(), tupleSort());
  }

  private Func makeFunc0(String name, Sort retSort, Sort... argSorts) {
    return funcCache.computeIfAbsent(name, n -> wrap(z3.mkFuncDecl(n, argSorts, retSort)));
  }

  private String tableFuncName(TableSym table) {
    return "t" + table.index();
  }

  private String pickFuncName(PickSym pick) {
    return "c" + pick.index();
  }

  private String predicateFuncName(PredicateSym pick) {
    return "p" + pick.index();
  }
}
