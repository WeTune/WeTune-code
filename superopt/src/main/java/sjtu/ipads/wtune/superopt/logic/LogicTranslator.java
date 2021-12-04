package sjtu.ipads.wtune.superopt.logic;

import com.google.common.collect.Sets;
import com.microsoft.z3.*;
import gnu.trove.list.TIntList;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.fragment.Symbols;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.uexpr.*;

import java.util.*;

import static java.lang.Integer.bitCount;
import static sjtu.ipads.wtune.common.utils.ArraySupport.*;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.uexpr.UKind.SQUASH;
import static sjtu.ipads.wtune.superopt.uexpr.UTerm.FUNC_IS_NULL_NAME;
import static sjtu.ipads.wtune.superopt.uexpr.UVar.VarKind.*;

class LogicTranslator {
  private final Substitution rule;
  private final UExprTranslationResult uExprs;
  private final Context z3;
  private final List<BoolExpr> assertions;

  LogicTranslator(UExprTranslationResult uExprs, Context z3) {
    this.rule = uExprs.rule();
    this.uExprs = uExprs;
    this.z3 = z3;
    this.assertions = new ArrayList<>();
  }

  boolean translate() {
    if (!trEqProposition()) return false;
    trConstraints();
    return true;
  }

  List<BoolExpr> assertions() {
    return assertions;
  }

  private boolean trEqProposition() {
    // fast reject: different output schema
    if (uExprs.schemaOf(uExprs.sourceFreeVar()) != uExprs.schemaOf(uExprs.targetFreeVar()))
      return false;

    final UTerm srcExpr = uExprs.sourceExpr();
    final UTerm tgtExpr = uExprs.targetExpr();
    final Set<UVar> srcBoundedVars = getBoundedVars(srcExpr);
    final Set<UVar> tgtBoundedVars = getBoundedVars(tgtExpr);
    final Set<UVar> boundedVars = Sets.intersection(srcBoundedVars, tgtBoundedVars);
    final int numBoundedVars = boundedVars.size();

    // fast reject: unaligned bounded variables
    // for example: Sum{x,y}(T(x) * S(y) * ...) = Sum{x,z}(T(x) * R(z) * ...)
    if (numBoundedVars != srcBoundedVars.size() && numBoundedVars != tgtBoundedVars.size())
      return false;

    assertions.add(z3.mkEq(trVar(uExprs.sourceFreeVar()), trVar(uExprs.targetFreeVar())));

    if (numBoundedVars == 0) {
      final ArithExpr srcFormula = trAsBag(srcExpr);
      final ArithExpr tgtFormula = trAsBag(tgtExpr);
      assertions.add(z3.mkNot(z3.mkEq(srcFormula, tgtFormula)));

    } else if (srcBoundedVars.size() == tgtBoundedVars.size()) { // Need apply theorem 5.1
      final ArithExpr srcFormula = trAsBag(srcExpr.subTerms().get(0));
      final ArithExpr tgtFormula = trAsBag(tgtExpr.subTerms().get(0));
      final Expr[] vars = map(boundedVars, this::trVar, Expr.class);
      final Expr body = z3.mkNot(z3.mkEq(srcFormula, tgtFormula));
      assertions.add(z3.mkExists(vars, body, 1, null, null, null, null));

    } else { // Need apply theorem 5.2
      // TODO
      return false;
    }

    return true;
  }

  private void trConstraints() {
    final Symbols srcSide = rule._0().symbols();

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

    final Set<UName> addedAttrsSub = new HashSet<>();
    for (Constraint c : rule.constraints().ofKind(AttrsSub)) {
      final UName name = uExprs.attrsDescOf(c.symbols()[0]).name();
      if (addedAttrsSub.add(name)) {
        trAttrsSub(c);
      }
    }

    for (TableDesc tableTerm : uExprs.tableTerms()) {
      trBasic(tableTerm.term());
    }
  }

  // Translate a u-expression, must not be summation.
  private ArithExpr trAsBag(UTerm uExpr) {
    final UKind kind = uExpr.kind();
    assert kind != UKind.SUMMATION;
    if (kind == UKind.ATOM) {
      return (ArithExpr) translateAtom(uExpr, false);

    } else if (kind.isBinary()) {
      final ArithExpr[] es = map(uExpr.subTerms(), this::trAsBag, ArithExpr.class);
      return kind == UKind.MULTIPLY ? z3.mkMul(es) : z3.mkAdd(es);

    } else if (kind.isUnary()) {
      final BoolExpr e = trAsSet(uExpr.subTerms().get(0));
      final IntNum one = z3.mkInt(1), zero = z3.mkInt(0);
      return (ArithExpr) (kind == SQUASH ? z3.mkITE(e, one, zero) : z3.mkITE(e, zero, one));

    } else {
      throw new IllegalArgumentException("unknown term");
    }
  }

  // Translate a u-expression inside squash/negation.
  private BoolExpr trAsSet(UTerm uExpr) {
    final UKind kind = uExpr.kind();
    assert kind != UKind.SUMMATION;
    if (kind == UKind.ATOM) {
      return (BoolExpr) translateAtom(uExpr, true);

    } else if (kind.isBinary()) {
      final BoolExpr[] es = map(uExpr.subTerms(), this::trAsSet, BoolExpr.class);
      return kind == UKind.MULTIPLY ? z3.mkAnd(es) : z3.mkOr(es);

    } else if (kind.isUnary()) {
      final BoolExpr e = trAsSet(uExpr.subTerms().get(0));
      return kind == SQUASH ? e : z3.mkNot(e);

    } else {
      throw new IllegalArgumentException("unknown term");
    }
  }

  private Expr translateAtom(UTerm atom, boolean asBool) {
    assert atom.kind() == UKind.ATOM;
    if (atom instanceof UTable) {
      final ArithExpr e = trTableAtom((UTable) atom);
      return asBool ? z3.mkGt(e, z3.mkInt(0)) : e;

    } else if (atom instanceof UPred) {
      final BoolExpr e = trPredAtom((UPred) atom);
      return asBool ? e : z3.mkITE(e, z3.mkInt(1), z3.mkInt(0));

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
    assert kind == FUNC || kind == EQ;
    if (kind == FUNC) {
      assert var.argument().length == 1;
      final FuncDecl func = z3.mkFuncDecl(var.name().toString(), tupleSort(), z3.getBoolSort());
      final Expr arg = trVar(var.argument()[0]);
      return (BoolExpr) func.apply(arg);

    } else { // kind == EQ
      assert var.argument().length == 2;
      final Expr lhs = trVar(var.argument()[0]);
      final Expr rhs = trVar(var.argument()[1]);
      return z3.mkEq(lhs, rhs);
    }
  }

  private Expr trVar(UVar var) {
    final UVar.VarKind kind = var.kind();
    final String name = var.name().toString();
    assert kind != FUNC && kind != EQ;
    if (kind == BASE) return z3.mkConst(name, tupleSort());
    if (kind == CONCAT) {
      final FuncDecl concatFunc = mkConcatFunc(var.argument().length);
      final Expr[] args = map(var.argument(), this::trVar, Expr.class);
      return concatFunc.apply(args);
    }
    if (kind == PROJ) {
      assert var.argument().length == 1;
      final FuncDecl projFunc = mkProjFunc(name);
      final int schema = uExprs.schemaOf(var.argument()[0]);
      final Expr arg = trVar(var.argument()[0]);
      return projFunc.apply(z3.mkInt(schema), arg);
    }
    throw new IllegalArgumentException("unknown var");
  }

  private void trBasic(UTable tableTerm) {
    final FuncDecl tableFunc = mkTableFunc(tableTerm.tableName().toString());
    final Expr tuple = z3.mkConst("x", tupleSort());
    final Expr[] vars = new Expr[] {tuple};
    final Expr body = z3.mkGe((ArithExpr) tableFunc.apply(tuple), z3.mkInt(0));
    final BoolExpr assertion = z3.mkForall(vars, body, 1, null, null, null, null);
    assertions.add(assertion);
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
    final BoolExpr assertion = z3.mkForall(vars, body, 1, null, null, null, null);
    assertions.add(assertion);
  }

  private void trAttrsSub(Constraint c) {
    final AttrsDesc attrsDesc = uExprs.attrsDescOf(c.symbols()[0]);
    final FuncDecl projFunc = mkProjFunc(attrsDesc.name().toString());
    final int restriction = uExprs.schemaOf(c.symbols()[1]);
    final TIntList schemas = attrsDesc.projectedSchemas;
    for (int i = 0, bound = schemas.size(); i < bound; ++i) {
      final int schema = schemas.get(i);
      assert (schema & restriction) != 0;
      if (schema == restriction) continue;

      final int numParts = bitCount(schema);
      final int partIndex = bitCount(schema & (restriction - 1));

      final IntNum s = z3.mkInt(schema), r = z3.mkInt(restriction);
      final FuncDecl concatFunc = mkConcatFunc(numParts);
      final Sort tupleSort = tupleSort();
      final Expr[] tuples = generate(numParts, j -> z3.mkConst("x" + j, tupleSort), Expr.class);
      final Expr pivotTuple = tuples[partIndex];
      final Expr concatTuple = concatFunc.apply(tuples);
      final BoolExpr body = z3.mkEq(projFunc.apply(s, concatTuple), projFunc.apply(r, pivotTuple));
      final BoolExpr assertion = z3.mkForall(tuples, body, 1, null, null, null, null);

      assertions.add(assertion);
    }
  }

  private void trNotNull(Constraint c) {
    final String tableName = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String attrsName = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final FuncDecl tableFunc = mkTableFunc(tableName);
    final FuncDecl projFunc = mkProjFunc(attrsName);
    final Expr tuple = z3.mkConst("x", tupleSort());
    final IntNum schema = z3.mkInt(uExprs.schemaOf(c.symbols()[0]));
    final BoolExpr p0 = z3.mkGt((ArithExpr) tableFunc.apply(tuple), z3.mkInt(0));
    final BoolExpr p1 = z3.mkNot((BoolExpr) mkIsNullFunc().apply(projFunc.apply(schema, tuple)));

    final Expr[] vars = new Expr[] {tuple};
    final Expr body = z3.mkIff(p0, p1);

    final BoolExpr assertion = z3.mkForall(vars, body, 1, null, null, null, null);
    assertions.add(assertion);
  }

  private void trUnique(Constraint c) {
    final String tableName = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String attrsName = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final FuncDecl projFunc = mkProjFunc(attrsName);
    final FuncDecl tableFunc = mkTableFunc(tableName);
    final IntNum zero = z3.mkInt(0), one = z3.mkInt(1);

    final Expr xTuple = z3.mkConst("x", tupleSort());
    final Expr[] vars0 = new Expr[] {xTuple};
    final BoolExpr body0 = z3.mkLe((ArithExpr) tableFunc.apply(xTuple), one);
    final BoolExpr assertion0 = z3.mkForall(vars0, body0, 1, null, null, null, null);
    assertions.add(assertion0);

    final Expr yTuple = z3.mkConst("y", tupleSort());
    final IntNum schema = z3.mkInt(uExprs.schemaOf(c.symbols()[0]));
    final BoolExpr b0 = z3.mkGt((ArithExpr) tableFunc.apply(xTuple), zero);
    final BoolExpr b1 = z3.mkGt((ArithExpr) tableFunc.apply(yTuple), zero);
    final BoolExpr b2 = z3.mkEq(projFunc.apply(schema, xTuple), projFunc.apply(schema, yTuple));
    final BoolExpr b4 = z3.mkEq(xTuple, yTuple);
    final Expr[] vars1 = new Expr[] {xTuple, yTuple};
    final BoolExpr body1 = z3.mkImplies(z3.mkAnd(b0, b1, b2), b4);
    final BoolExpr assertion1 = z3.mkForall(vars1, body1, 1, null, null, null, null);
    assertions.add(assertion1);
  }

  private void trReferences(Constraint c) {
    final String tableName0 = uExprs.tableDescOf(c.symbols()[0]).term().tableName().toString();
    final String tableName1 = uExprs.tableDescOf(c.symbols()[2]).term().tableName().toString();
    final String attrsName0 = uExprs.attrsDescOf(c.symbols()[1]).name().toString();
    final String attrsName1 = uExprs.attrsDescOf(c.symbols()[3]).name().toString();
    final int schema0 = uExprs.schemaOf(c.symbols()[0]), schema1 = uExprs.schemaOf(c.symbols()[2]);

    final FuncDecl tableFunc0 = mkTableFunc(tableName0), tableFunc1 = mkTableFunc(tableName1);
    final FuncDecl projFunc0 = mkProjFunc(attrsName0), projFunc1 = mkProjFunc(attrsName1);
    final IntNum zero = z3.mkInt(0);

    final Expr xTuple = z3.mkConst("x", tupleSort()), yTuple = z3.mkConst("y", tupleSort());
    final IntNum xSchema = z3.mkInt(schema0), ySchema = z3.mkInt(schema1);
    final Expr xProj = projFunc0.apply(xSchema, xTuple), yProj = projFunc1.apply(ySchema, yTuple);
    final BoolExpr b0 = z3.mkGt((ArithExpr) tableFunc0.apply(xTuple), zero);
    final BoolExpr b1 = z3.mkNot((BoolExpr) mkIsNullFunc().apply(xProj));
    final BoolExpr b2 = z3.mkGt((ArithExpr) tableFunc1.apply(yTuple), zero);
    final BoolExpr b3 = z3.mkNot((BoolExpr) mkIsNullFunc().apply(yProj));
    final BoolExpr b4 = z3.mkEq(xProj, yProj);
    final Expr[] innerVars = new Expr[] {yTuple}, outerVars = new Expr[] {xTuple};
    final BoolExpr innerBody = z3.mkAnd(b2, b3, b4);
    final BoolExpr inner = z3.mkExists(innerVars, innerBody, 1, null, null, null, null);
    final BoolExpr outerBody = z3.mkImplies(z3.mkAnd(b0, b1), inner);
    final BoolExpr outer = z3.mkForall(outerVars, outerBody, 1, null, null, null, null);

    assertions.add(outer);
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

  private static Set<UVar> getBoundedVars(UTerm expr) {
    // Returns the summation variables for a summation, otherwise an empty list.
    if (expr.kind() == UKind.SUMMATION) return ((USum) expr).boundedVars();
    else return Collections.emptySet();
  }
}
