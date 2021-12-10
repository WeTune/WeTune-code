package sjtu.ipads.wtune.superopt.logic;

import com.microsoft.z3.*;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.fragment.Symbols;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.uexpr.*;

import java.util.*;

import static com.google.common.collect.Sets.difference;
import static sjtu.ipads.wtune.common.utils.ArraySupport.*;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.TABLE;
import static sjtu.ipads.wtune.superopt.logic.LogicSupport.FAST_REJECTED;
import static sjtu.ipads.wtune.superopt.uexpr.UKind.*;
import static sjtu.ipads.wtune.superopt.uexpr.UTerm.FUNC_IS_NULL_NAME;
import static sjtu.ipads.wtune.superopt.uexpr.UVar.VarKind.*;

class LogicProver {
  private final Substitution rule;
  private final UExprTranslationResult uExprs;
  private final Context z3;
  private final List<BoolExpr> constraints;

  LogicProver(UExprTranslationResult uExprs, Context z3) {
    this.rule = uExprs.rule();
    this.uExprs = uExprs;
    this.z3 = z3;
    this.constraints = new ArrayList<>();
  }

  int proveEq() {
    // fast reject: different output schema
    final int srcOutSchema = uExprs.schemaOf(uExprs.sourceFreeVar());
    final int tgtOutSchema = uExprs.schemaOf(uExprs.targetFreeVar());
    assert srcOutSchema != 0 && tgtOutSchema != 0;
    if (srcOutSchema != tgtOutSchema) return FAST_REJECTED;
    // fast reject: unaligned variables
    // master: the side with more bounded variables, or the source side if the numbers are equal
    // master: the side with less bounded variables, or the target side if the numbers are equal
    final UTerm srcTerm = uExprs.sourceExpr(), tgtTerm = uExprs.targetExpr();
    final UTerm masterTerm = getMaster(srcTerm, tgtTerm);
    final UTerm slaveTerm = getSlave(srcTerm, tgtTerm);
    if (!getBoundedVars(masterTerm).containsAll(getBoundedVars(slaveTerm))) return FAST_REJECTED;

    trConstraints();
    return proveEq0(masterTerm, slaveTerm);
  }

  private void trConstraints() {
    final Symbols srcSide = rule._0().symbols();

    for (var tableSym : srcSide.symbolsOf(TABLE)) {
      trBasic(uExprs.tableDescOf(tableSym).term());
    }

    for (Constraint c : rule.constraints().ofKind(AttrsEq)) {
      if (c.symbols()[0].ctx() == srcSide && c.symbols()[1].ctx() == srcSide) {
        trAttrsEq(c);
      }
    }

    for (Constraint c : rule.constraints().ofKind(NotNull)) {
      trNotNull(c);
    }

    for (Constraint c : rule.constraints().ofKind(Unique)) {
      trUnique(c);
    }

    for (Constraint c : rule.constraints().ofKind(Reference)) {
      trReferences(c);
    }

    constraints.add(z3.mkEq(trVar(uExprs.sourceFreeVar()), trVar(uExprs.targetFreeVar())));
  }

  private void trBasic(UTable tableTerm) {
    final FuncDecl tableFunc = mkTableFunc(tableTerm.tableName().toString());
    final Expr tuple = z3.mkConst("x", tupleSort());
    final Expr[] vars = new Expr[] {tuple};
    final Expr body = z3.mkGe((ArithExpr) tableFunc.apply(tuple), zero());
    final BoolExpr assertion = mkForall(vars, body);
    constraints.add(assertion);
  }

  private void trAttrsEq(Constraint c) {
    final int schema0 = uExprs.schemaOf(rule.constraints().sourceOf(c.symbols()[0]));
    final int schema1 = uExprs.schemaOf(rule.constraints().sourceOf(c.symbols()[1]));
    if (schema0 == schema1) return;

    final IntNum s0 = z3.mkInt(schema0), s1 = z3.mkInt(schema1);
    final FuncDecl projFunc = mkProjFunc(uExprs.attrsDescOf(c.symbols()[0]).name().toString());
    final Expr tuple = z3.mkConst("x", tupleSort());
    final Expr[] vars = new Expr[] {tuple};
    final BoolExpr body = z3.mkEq(projFunc.apply(s0, tuple), projFunc.apply(s1, tuple));
    final BoolExpr assertion = mkForall(vars, body);
    constraints.add(assertion);
  }

  private void trNotNull(Constraint c) {
    final String tableName = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String attrsName = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final FuncDecl tableFunc = mkTableFunc(tableName);
    final FuncDecl projFunc = mkProjFunc(attrsName);
    final Expr tuple = z3.mkConst("x", tupleSort());
    final IntNum schema = z3.mkInt(uExprs.schemaOf(c.symbols()[0]));
    final BoolExpr p0 = z3.mkGt((ArithExpr) tableFunc.apply(tuple), zero());
    final BoolExpr p1 = z3.mkNot((BoolExpr) mkIsNullFunc().apply(projFunc.apply(schema, tuple)));

    final Expr[] vars = new Expr[] {tuple};
    final Expr body = z3.mkIff(p0, p1);

    final BoolExpr assertion = mkForall(vars, body);
    constraints.add(assertion);
  }

  private void trUnique(Constraint c) {
    final String tableName = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String attrsName = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final FuncDecl projFunc = mkProjFunc(attrsName);
    final FuncDecl tableFunc = mkTableFunc(tableName);

    final Expr xTuple = z3.mkConst("x", tupleSort());
    final Expr[] vars0 = new Expr[] {xTuple};
    final BoolExpr body0 = z3.mkLe((ArithExpr) tableFunc.apply(xTuple), one());
    final BoolExpr assertion0 = mkForall(vars0, body0);
    constraints.add(assertion0);

    final Expr yTuple = z3.mkConst("y", tupleSort());
    final IntNum schema = z3.mkInt(uExprs.schemaOf(c.symbols()[0]));
    final BoolExpr b0 = z3.mkGt((ArithExpr) tableFunc.apply(xTuple), zero());
    final BoolExpr b1 = z3.mkGt((ArithExpr) tableFunc.apply(yTuple), zero());
    final BoolExpr b2 = z3.mkEq(projFunc.apply(schema, xTuple), projFunc.apply(schema, yTuple));
    final BoolExpr b4 = z3.mkEq(xTuple, yTuple);
    final Expr[] vars1 = new Expr[] {xTuple, yTuple};
    final BoolExpr body1 = z3.mkImplies(z3.mkAnd(b0, b1, b2), b4);
    final BoolExpr assertion1 = mkForall(vars1, body1);
    constraints.add(assertion1);
  }

  private void trReferences(Constraint c) {
    final String tableName0 = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String tableName1 = uExprs.tableDescOf(c.symbols()[2]).term().tableName().toString();
    final String attrsName0 = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final String attrsName1 = uExprs.attrsDescOf(c.symbols()[3]).name().toString();
    final int schema0 = uExprs.schemaOf(c.symbols()[0]), schema1 = uExprs.schemaOf(c.symbols()[2]);

    final FuncDecl tableFunc0 = mkTableFunc(tableName0), tableFunc1 = mkTableFunc(tableName1);
    final FuncDecl projFunc0 = mkProjFunc(attrsName0), projFunc1 = mkProjFunc(attrsName1);
    final IntNum zero = zero();

    final Expr xTuple = z3.mkConst("x", tupleSort()), yTuple = z3.mkConst("y", tupleSort());
    final IntNum xSchema = z3.mkInt(schema0), ySchema = z3.mkInt(schema1);
    final Expr xProj = projFunc0.apply(xSchema, xTuple), yProj = projFunc1.apply(ySchema, yTuple);
    final BoolExpr b0 = z3.mkGt((ArithExpr) tableFunc0.apply(xTuple), zero);
    final BoolExpr b1 = z3.mkNot((BoolExpr) mkIsNullFunc().apply(xProj));
    final BoolExpr b2 = z3.mkGt((ArithExpr) tableFunc1.apply(yTuple), zero);
    final BoolExpr b3 = z3.mkNot((BoolExpr) mkIsNullFunc().apply(yProj));
    final BoolExpr b4 = z3.mkEq(xProj, yProj);
    final Expr[] innerVars = new Expr[] {yTuple}, outerVars = new Expr[] {xTuple};
    final BoolExpr body = z3.mkImplies(z3.mkAnd(b0, b1), mkExists(innerVars, z3.mkAnd(b2, b3, b4)));
    final BoolExpr assertion = mkForall(outerVars, body);

    constraints.add(assertion);
  }

  private int proveEq0(UTerm masterTerm, UTerm slaveTerm) {
    final UTerm masterBody = getBody(masterTerm);
    final UTerm slaveBody = getBody(slaveTerm);
    final Set<UVar> masterVars = getBoundedVars(masterTerm);
    final Set<UVar> slaveVars = getBoundedVars(slaveTerm);
    final Solver solver = z3.mkSolver();
    solver.add(constraints.toArray(BoolExpr[]::new));

    // simple case: E = E' or Sum{x}(E) = Sum{x}(E') ==> tr(E) = tr(E')
    if (masterVars.size() == slaveVars.size()) {
      final ArithExpr masterFormula = trAsBag(masterBody);
      final ArithExpr slaveFormula = trAsBag(slaveBody);
      return trResult(check(solver, z3.mkNot(z3.mkEq(masterFormula, slaveFormula))));
    }

    /*
     complex cases: Sum{x}(X) = Sum{x,y}(Y * Z), where Y is the term using y.
     Need to apply Theorem 5.2.

     The target is to proveP is valid:
     P := (X != Y => X = 0 /\ Sum{y}(Z(y)) = 0)     | Q0
          /\ (X = Y => X = 0 \/ Sum{y}(Z(y) = 1))   | Q1
     We need to prove Q0 and Q1 are both valid.

     To prove Q0's validity, we just need to prove
       (X != Y => X = 0) /\ (X != Y /\ X = 0 /\ Y != 0 => Sum{y}(Z(y)) = 0)
     so we prove
       q0: X != Y /\ X != 0 is unsat
       q1: X != Y /\ X = 0 /\ Sum{y}(Z(y)) != 0 is unsat

     To prove Q1's validity, we need to prove
          X = Y /\ X != 0 /\ Sum{y}(Z(y)) != 1 is unsat
     Sum{y}(Z(y)) != 1 can be broken into (q2 \/ q3 \/ q4), where
       q2: \forall y. Z(y) = 0
       q3: \exists y. Z(y) > 1
       q4: \exist y,y'. y != y' /\ Z(y) = 1 /\ Z(y') = 1
       We prove (X = Y /\ X != 0 /\ q2), (X = Y /\ X != 0 /\ q3) and (X = Y /\ X != 0 /\ q4) are all unsat.

     There will be 5 SMT invocations in total.
    */

    Status answer;
    final HashSet<UVar> diffVars = new HashSet<>(difference(masterVars, slaveVars));
    final var pair = separateFactors(masterBody, diffVars);
    final UTerm exprX = slaveBody, exprY = pair.getLeft(), exprZ = pair.getRight();
    final ArithExpr valueX = trAsBag(exprX);
    final ArithExpr valueY = trAsBag(exprY);
    final BoolExpr boolX = trAsSet(exprX);
    final BoolExpr boolY = trAsSet(exprY);
    final BoolExpr boolZ = trAsSet(exprZ);

    // q0: X != Y /\ X != 0
    final BoolExpr eqXY = z3.mkEq(valueX, valueY);
    final BoolExpr neqXY = z3.mkNot(eqXY);
    answer = check(solver, neqXY, boolX);
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q1: X != Y /\ X = 0 /\ Y != 0 /\ Z != 0 (X != Y can be collapsed)
    answer = check(solver, z3.mkNot(boolX), boolY, boolZ);
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q2: X = Y /\ X != 0 /\ \forall y. Z(y) = 0
    final Expr[] ys = map(diffVars, this::trVar, Expr.class);
    final BoolExpr eqZ0 = mkForall(ys, z3.mkNot(boolZ));
    answer = check(solver, eqXY, boolX, eqZ0);
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q3: X = Y /\ X != 0 /\ Z(y) > 1
    final ArithExpr valueZ = trAsBag(exprZ);
    answer = check(solver, eqXY, boolX, z3.mkGt(valueZ, one()));
    if (answer != Status.UNSATISFIABLE) return trResult(answer);

    // q4: X = Y /\ Z(y) != 0 /\ Z(y') != 0 /\ y != y'
    final FuncDecl funcY = z3.mkFuncDecl("Z", map(ys, Expr::getSort, Sort.class), z3.getBoolSort());
    solver.add(mkForall(ys, z3.mkEq(funcY.apply(ys), boolZ)));
    final Expr[] ys1 = map(diffVars, it -> trVar(UVar.mkBase(UName.mk(it + "_"))), Expr.class);
    solver.add(generate(ys.length, i -> z3.mkNot(z3.mkEq(ys[i], ys1[i])), BoolExpr.class));
    answer = check(solver, eqXY, boolX, (BoolExpr) funcY.apply(ys), (BoolExpr) funcY.apply(ys1));

    return trResult(answer);
  }

  // Translate a u-expression, must not be summation.
  private ArithExpr trAsBag(UTerm uExpr) {
    final UKind kind = uExpr.kind();
    assert kind != UKind.SUMMATION;
    if (kind == UKind.ATOM) {
      return (ArithExpr) trAtom(uExpr, false);

    } else if (kind == MULTIPLY) {
      if (uExpr.subTerms().isEmpty()) return one();

      final List<BoolExpr> bools = new ArrayList<>(uExpr.subTerms().size());
      final List<ArithExpr> nums = new ArrayList<>(uExpr.subTerms().size());
      for (UTerm term : uExpr.subTerms()) {
        if (term instanceof UTable) nums.add((ArithExpr) trAtom(term, false));
        else bools.add(trAsSet(term));
      }
      final ArithExpr[] factors = nums.toArray(ArithExpr[]::new);
      if (bools.isEmpty()) return z3.mkMul(factors);
      else {
        final BoolExpr[] preconditions = bools.toArray(BoolExpr[]::new);
        return (ArithExpr) z3.mkITE(z3.mkAnd(preconditions), z3.mkMul(factors), zero());
      }

    } else if (kind == ADD) {
      return z3.mkAdd(map(uExpr.subTerms(), this::trAsBag, ArithExpr.class));

    } else if (kind.isUnary()) {
      final BoolExpr e = trAsSet(uExpr.subTerms().get(0));
      return (ArithExpr) (kind == SQUASH ? z3.mkITE(e, one(), zero()) : z3.mkITE(e, zero(), one()));

    } else {
      throw new IllegalArgumentException("unknown term");
    }
  }

  // Translate a u-expression inside squash/negation.
  private BoolExpr trAsSet(UTerm uExpr) {
    final UKind kind = uExpr.kind();

    if (kind == UKind.ATOM) {
      return (BoolExpr) trAtom(uExpr, true);

    } else if (kind.isBinary()) {
      if (uExpr.subTerms().isEmpty()) return z3.mkBool(true);

      final BoolExpr[] es = map(uExpr.subTerms(), this::trAsSet, BoolExpr.class);
      return kind == UKind.MULTIPLY ? z3.mkAnd(es) : z3.mkOr(es);

    } else if (kind == SUMMATION) {
      final USum sum = (USum) uExpr;
      final BoolExpr body = trAsSet(sum.body());
      final Expr[] vars = map(sum.boundedVars(), this::trVar, Expr.class);
      return mkExists(vars, body);

    } else if (kind.isUnary()) {
      final BoolExpr e = trAsSet(uExpr.subTerms().get(0));
      return kind == SQUASH ? e : z3.mkNot(e);

    } else {
      throw new IllegalArgumentException("unknown term");
    }
  }

  private Expr trVar(UVar var) {
    final UVar.VarKind kind = var.kind();
    final String name = var.name().toString();
    assert kind != FUNC && kind != UVar.VarKind.EQ;
    if (kind == BASE) return z3.mkConst(name, tupleSort());
    if (kind == CONCAT) {
      final FuncDecl concatFunc = mkConcatFunc(var.args().length);
      final Expr[] args = map(var.args(), this::trVar, Expr.class);
      return concatFunc.apply(args);
    }
    if (kind == PROJ) {
      assert var.args().length == 1;
      final FuncDecl projFunc = mkProjFunc(name);
      final int schema = uExprs.schemaOf(var.args()[0]);
      final Expr arg = trVar(var.args()[0]);
      return projFunc.apply(z3.mkInt(schema), arg);
    }
    throw new IllegalArgumentException("unknown var");
  }

  private Expr trAtom(UTerm atom, boolean asBool) {
    assert atom.kind() == UKind.ATOM;
    if (atom instanceof UTable) {
      final ArithExpr e = trTableAtom((UTable) atom);
      return asBool ? z3.mkGt(e, zero()) : e;

    } else if (atom instanceof UPred) {
      final BoolExpr e = trPredAtom((UPred) atom);
      return asBool ? e : z3.mkITE(e, one(), zero());

    } else {
      throw new IllegalArgumentException("unknown atom");
    }
  }

  private ArithExpr trTableAtom(UTable tableTerm) {
    final String tableName = tableTerm.tableName().toString();
    final String varName = tableTerm.var().name().toString();
    final FuncDecl func = mkTableFunc(tableName);
    final Expr var = z3.mkConst(varName, tupleSort());
    return (ArithExpr) func.apply(var);
  }

  private BoolExpr trPredAtom(UPred predTerm) {
    final UVar var = predTerm.var();
    final UVar.VarKind kind = var.kind();
    assert kind == FUNC || kind == UVar.VarKind.EQ;
    if (kind == FUNC) {
      assert var.args().length == 1;
      final FuncDecl func = z3.mkFuncDecl(var.name().toString(), tupleSort(), z3.getBoolSort());
      final Expr arg = trVar(var.args()[0]);
      return (BoolExpr) func.apply(arg);

    } else { // kind == EQ
      assert var.args().length == 2;
      final Expr lhs = trVar(var.args()[0]);
      final Expr rhs = trVar(var.args()[1]);
      return z3.mkEq(lhs, rhs);
    }
  }

  private IntNum one() {
    return z3.mkInt(1);
  }

  private IntNum zero() {
    return z3.mkInt(0);
  }

  private Quantifier mkForall(Expr[] vars, Expr body) {
    return z3.mkForall(vars, body, 1, null, null, null, null);
  }

  private Quantifier mkExists(Expr[] vars, Expr body) {
    return z3.mkExists(vars, body, 1, null, null, null, null);
  }

  private Sort tupleSort() {
    return z3.mkUninterpretedSort("Tuple");
  }

  private FuncDecl mkTableFunc(String name) {
    return z3.mkFuncDecl(name, tupleSort(), z3.getIntSort());
  }

  private FuncDecl mkProjFunc(String name) {
    return z3.mkFuncDecl(name, new Sort[] {z3.getIntSort(), tupleSort()}, tupleSort());
  }

  private FuncDecl mkIsNullFunc() {
    return z3.mkFuncDecl(FUNC_IS_NULL_NAME, tupleSort(), z3.getBoolSort());
  }

  private FuncDecl mkConcatFunc(int arity) {
    final Sort[] argSorts = repeat(tupleSort(), arity);
    return z3.mkFuncDecl("concat" + arity, argSorts, tupleSort());
  }

  private int trResult(Status res) {
    if (res == Status.UNSATISFIABLE) return LogicSupport.EQ;
    else if (res == Status.SATISFIABLE) return LogicSupport.NEQ;
    else return LogicSupport.UNKNOWN;
  }

  private Status check(Solver solver, BoolExpr... exprs) {
    LogicSupport.incrementNumInvocations();
    solver.push();
    solver.add(exprs);
    final Status res = solver.check();
    solver.pop();
    return res;
    //    return solver.check(exprs);
  }

  private static UTerm getBody(UTerm expr) {
    if (expr.kind() == SUMMATION) return ((USum) expr).body();
    else return expr;
  }

  private static UTerm getMaster(UTerm e0, UTerm e1) {
    final Set<UVar> vars0 = getBoundedVars(e0);
    final Set<UVar> vars1 = getBoundedVars(e1);
    if (vars0.size() >= vars1.size()) return e0;
    else return e1;
  }

  private static UTerm getSlave(UTerm e0, UTerm e1) {
    final Set<UVar> vars0 = getBoundedVars(e0);
    final Set<UVar> vars1 = getBoundedVars(e1);
    if (vars0.size() < vars1.size()) return e0;
    else return e1;
  }

  private static Pair<UTerm, UTerm> separateFactors(UTerm mul, Set<UVar> vars) {
    final List<UTerm> factors = mul.subTerms();
    final List<UTerm> termsY = new ArrayList<>(factors.size());
    final List<UTerm> termsZ = new ArrayList<>(factors.size());
    for (UTerm term : factors) {
      if (any(vars, term::isUsing)) termsZ.add(term);
      else termsY.add(term);
    }
    return Pair.of(UMul.mk(termsY), UMul.mk(termsZ));
  }

  private static Set<UVar> getBoundedVars(UTerm expr) {
    // Returns the summation variables for a summation, otherwise an empty list.
    if (expr.kind() == UKind.SUMMATION) return ((USum) expr).boundedVars();
    else return Collections.emptySet();
  }
}
