package sjtu.ipads.wtune.prover.utils;

import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.EqPredTerm;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

import java.util.Arrays;
import java.util.Collection;

import static sjtu.ipads.wtune.prover.uexpr.UExpr.*;
import static sjtu.ipads.wtune.prover.utils.Constants.FREE_VAR;
import static sjtu.ipads.wtune.prover.utils.Constants.NULL;

public final class UExprUtils {
  private UExprUtils() {}

  public static boolean isReflexivity(UExpr pred) {
    if (pred.kind() != Kind.EQ_PRED) return false;
    final EqPredTerm eq = (EqPredTerm) pred;
    return eq.lhs().equals(eq.rhs());
  }

  public static boolean isConstantVar(Var t) {
    if (t.isConstant()) return true;
    if (t.isBase() && t.name().toString().equals(FREE_VAR)) return true;
    if (t.isProjected()) return isConstantVar(t.base()[0]);
    return false;
  }

  public static boolean isNull(Var t) {
    return t.uses(NULL);
  }

  public static UExpr mkIsNull(Var t) {
    return eqPred(t, NULL);
  }

  public static UExpr mkNotNull(Var t) {
    return not(mkIsNull(t));
  }

  public static UExpr mkNullSafeEq(Var t0, Var t1) {
    return mul(mkNotNull(t1), eqPred(t0, t1));
  }

  public static UExpr mkProduct(Collection<UExpr> exprs) {
    return mkProduct(exprs, false);
  }

  public static UExpr mkProduct(Collection<UExpr> exprs, boolean copyTerms) {
    if (copyTerms) return exprs.stream().map(UExpr::copy).reduce(UExpr::mul).orElseThrow();
    else return exprs.stream().reduce(UExpr::mul).orElseThrow();
  }

  public static Var[] substArgs(Var[] args, Var t, Var rep) {
    final Var[] newArgs = Arrays.copyOf(args, args.length);
    boolean changed = false;
    for (int i = 0; i < args.length; i++) {
      newArgs[i] = args[i].subst(t, rep);
      changed |= newArgs[i] != args[i];
    }
    if (!changed) return args;
    else return newArgs;
  }

  public static StringBuilder interpolateVars(String template, Var[] vars, StringBuilder builder) {
    int start = 0;
    for (Var arg : vars) {
      final int end = template.indexOf("?", start);
      builder.append(template, start, end - 1);
      builder.append(arg);
      start = end + 2;
    }
    builder.append(template, start, template.length());

    return builder;
  }

  public static Disjunction renameVars(Disjunction d, String prefix) {
    renameVars0(d, prefix, 0);
    return d;
  }

  private static int renameVars0(Disjunction d, String prefix, int startIdx) {
    if (d == null) return startIdx;

    int idx = startIdx;
    for (Conjunction c : d) {
      for (Var var : c.vars()) c.subst(var, Var.mkBase(prefix + idx++));
      idx = renameVars0(c.squash(), prefix, idx);
      idx = renameVars0(c.neg(), prefix, idx);
    }

    return idx;
  }
}
