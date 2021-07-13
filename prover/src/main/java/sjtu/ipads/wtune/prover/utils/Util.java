package sjtu.ipads.wtune.prover.utils;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.stream.IntStream.range;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.utils.Constants.FREE_VAR;
import static sjtu.ipads.wtune.prover.utils.Constants.NOT_NULL_PRED;
import static sjtu.ipads.wtune.prover.utils.Constants.NULL_TUPLE;
import static sjtu.ipads.wtune.prover.utils.Constants.TEMP_VAR_PREFIX_0;
import static sjtu.ipads.wtune.prover.utils.Constants.TEMP_VAR_PREFIX_1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

public final class Util {

  private Util() {}

  public static boolean isNullTuple(Tuple t) {
    return t.equals(NULL_TUPLE);
  }

  public static boolean isConstantTuple(Tuple t) {
    if (t.isConstant()) return true;
    if (t.isBase() && t.name().toString().equals(FREE_VAR)) return true;
    if (t.isProjected()) return isConstantTuple(t.base()[0]);
    return false;
  }

  public static boolean isReflexivity(UExpr pred) {
    if (pred.kind() != Kind.EQ_PRED) return false;
    final EqPredTerm eqPred = (EqPredTerm) pred;
    return eqPred.left().equals(eqPred.right());
  }

  public static boolean isContradictory(UExpr pred) {
    if (pred.kind() != Kind.EQ_PRED) return false;
    final EqPredTerm eqPred = (EqPredTerm) pred;
    return eqPred.left().isConstant()
        && eqPred.right().isConstant()
        && !eqPred.left().equals(eqPred.right());
  }

  public static boolean isNotNullPredOf(UExpr pred, Tuple tuple) {
    if (pred.kind() != Kind.PRED) return false;
    final UninterpretedPredTerm p = (UninterpretedPredTerm) pred;
    return p.name().toString().equals(NOT_NULL_PRED)
        && p.tuple().length == 1
        && p.tuple()[0].equals(tuple);
  }

  public static boolean compareTable(UExpr e0, UExpr e1) {
    if (!(e0 instanceof TableTerm) || !(e1 instanceof TableTerm)) return false;
    final TableTerm t0 = (TableTerm) e0, t1 = (TableTerm) e1;
    return Objects.equals(t0.name(), t1.name()) && Objects.equals(t0.tuple(), t1.tuple());
  }

  public static boolean comparePred(UExpr e0, UExpr e1) {
    if (e0 instanceof EqPredTerm && e1 instanceof EqPredTerm) {
      final EqPredTerm eq0 = (EqPredTerm) e0, eq1 = (EqPredTerm) e1;
      return (eq0.left().equals(eq1.left()) && eq0.right().equals(eq1.right()))
          || (eq0.left().equals(eq1.right()) && eq0.right().equals(eq1.left()));

    } else if (e0 instanceof UninterpretedPredTerm && e1 instanceof UninterpretedPredTerm) {
      final UninterpretedPredTerm ex0 = (UninterpretedPredTerm) e0;
      final UninterpretedPredTerm ex1 = (UninterpretedPredTerm) e1;
      final Tuple[] args0 = ex0.tuple(), args1 = ex1.tuple();
      return ex0.name().equals(ex1.name()) && Arrays.equals(args0, args1);

    } else return false;
  }

  public static List<UExpr> calcEqClosure(List<UExpr> preds) {
    if (preds.isEmpty()) return preds;

    final Congruence<Tuple> congruence = Congruence.make(preds);
    final List<UExpr> ret = new ArrayList<>(preds);

    for (UExpr pred : preds) {
      if (pred.kind() != Kind.PRED) continue;

      final UninterpretedPredTerm p = (UninterpretedPredTerm) pred;
      for (List<Tuple> tuples : cartesianProduct(listMap(congruence::getClass, p.tuple())))
        ret.add(UExpr.uninterpretedPred(p.name().toString(), tuples.toArray(Tuple[]::new)));
    }

    return ret;
  }

  public static Tuple[] subst(Tuple[] args, Tuple t, Tuple rep) {
    final Tuple[] newArgs = Arrays.copyOf(args, args.length);
    boolean changed = false;
    for (int i = 0; i < args.length; i++) {
      newArgs[i] = args[i].subst(t, rep);
      changed |= newArgs[i] != args[i];
    }
    if (!changed) return args;
    else return newArgs;
  }

  public static StringBuilder interpolateToString(
      String template, Tuple[] tuples, StringBuilder builder) {
    int start = 0;
    for (Tuple arg : tuples) {
      final int end = template.indexOf("?", start);
      builder.append(template, start, end - 1);
      builder.append(arg);
      start = end + 2;
    }
    builder.append(template, start, template.length());

    return builder;
  }

  public static <T> List<T> arrange(List<T> ts, int[] arrangement) {
    final List<T> matching = new ArrayList<>(arrangement.length);
    for (int i : arrangement) matching.add(ts.get(i));
    return matching;
  }

  public static Map<String, List<TableTerm>> groupTables(Conjunction c) {
    return c.tables().stream()
        .map(it -> (TableTerm) it)
        .collect(Collectors.groupingBy(it -> it.name().toString()));
  }

  public static String ownerTableOf(Constraint constraint) {
    return constraint.columns().get(0).tableName();
  }

  public static List<Tuple> makeFreshVars(String prefix, int count) {
    return range(0, count).mapToObj(i -> Tuple.make(prefix + i)).toList();
  }

  public static Conjunction minimizeVars(Conjunction c) {
    c.vars().removeIf(Predicate.not(c::usesInBody));
    return c;
  }

  public static Conjunction substBoundedVars(Conjunction c, List<Tuple> tuples) {
    final Conjunction copy = c.copy();
    final List<Tuple> vars = copy.vars();
    for (int i = 0, bound = tuples.size(); i < bound; i++) {
      copy.subst(vars.get(i), tuples.get(i));
    }
    return copy;
  }

  public static Disjunction renameVars(Disjunction d, String prefix) {
    renameVars0(d, prefix, 0);
    return d;
  }

  private static int renameVars0(Disjunction d, String prefix, int startIdx) {
    if (d == null) return startIdx;

    int idx = startIdx;
    for (Conjunction c : d) {
      for (Tuple var : c.vars()) c.subst(var, Tuple.make(prefix + idx++));
      idx = renameVars0(c.squash(), prefix, idx);
      idx = renameVars0(c.neg(), prefix, idx);
    }

    return idx;
  }

  public static boolean isMatchedUninterpretedPred(
      UExpr pred0, UExpr pred1, Congruence<Tuple> congruence) {
    if (pred0.kind() != Kind.PRED || pred1.kind() != Kind.PRED) return false;

    final UninterpretedPredTerm p0 = (UninterpretedPredTerm) pred0;
    final UninterpretedPredTerm p1 = (UninterpretedPredTerm) pred1;

    if (!Objects.equals(p0.name(), p1.name())) return false;

    final Tuple[] args0 = p0.tuple(), args1 = p1.tuple();
    if (args0.length != args1.length) return false;

    for (int i = 0, bound = args0.length; i < bound; i++)
      if (!congruence.isCongruent(args0[i], args1[i])) {
        return false;
      }

    return true;
  }

  private static final List<Tuple> TEMP_VARS_0 = makeFreshVars(TEMP_VAR_PREFIX_0, 100);
  private static final List<Tuple> TEMP_VARS_1 = makeFreshVars(TEMP_VAR_PREFIX_1, 100);

  public static List<Tuple> makeTempVars0(int start, int count) {
    return TEMP_VARS_0.subList(start, start + count);
  }

  public static List<Tuple> makeTempVars1(int count) {
    return TEMP_VARS_1.subList(0, count);
  }
}
