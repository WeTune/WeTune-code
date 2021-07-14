package sjtu.ipads.wtune.prover.utils;

import static sjtu.ipads.wtune.prover.utils.Constants.FREE_VAR;
import static sjtu.ipads.wtune.prover.utils.Constants.NOT_NULL_PRED;
import static sjtu.ipads.wtune.prover.utils.Constants.NULL_VAR;

import java.util.Arrays;
import java.util.Collection;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.UExpr.Kind;
import sjtu.ipads.wtune.prover.uexpr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.uexpr.Var;

public final class Util {
  private Util() {}

  public static boolean isNullTuple(Var t) {
    return t.equals(NULL_VAR);
  }

  public static boolean isConstantTuple(Var t) {
    if (t.isConstant()) return true;
    if (t.isBase() && t.name().toString().equals(FREE_VAR)) return true;
    if (t.isProjected()) return isConstantTuple(t.base()[0]);
    return false;
  }

  public static boolean isNotNullPredOf(UExpr pred, Var var) {
    if (pred.kind() != Kind.PRED) return false;
    final UninterpretedPredTerm p = (UninterpretedPredTerm) pred;
    return p.name().toString().equals(NOT_NULL_PRED)
        && p.tuple().length == 1
        && p.tuple()[0].equals(var);
  }

  public static Var[] substVar(Var[] args, Var t, Var rep) {
    final Var[] newArgs = Arrays.copyOf(args, args.length);
    boolean changed = false;
    for (int i = 0; i < args.length; i++) {
      newArgs[i] = args[i].subst(t, rep);
      changed |= newArgs[i] != args[i];
    }
    if (!changed) return args;
    else return newArgs;
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

  public static UExpr mkProduct(Collection<UExpr> exprs) {
    return mkProduct(exprs, false);
  }

  public static UExpr mkProduct(Collection<UExpr> exprs, boolean copyTerms) {
    if (copyTerms) return exprs.stream().map(UExpr::copy).reduce(UExpr::mul).orElseThrow();
    else return exprs.stream().reduce(UExpr::mul).orElseThrow();
  }

  public static StringBuilder interpolateToString(
      String template, Var[] vars, StringBuilder builder) {
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
}
