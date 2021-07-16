package sjtu.ipads.wtune.prover.normalform;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.Kind.TABLE;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.mul;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.sum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

final class ConjunctionImpl implements Conjunction {
  private static final Conjunction EMPTY =
      new ConjunctionImpl(emptyList(), emptyList(), emptyList(), null, null);

  private final List<Var> vars;
  private final List<UExpr> predicates;
  private final Disjunction negation;
  private final Disjunction squash;
  private final List<UExpr> tables;

  private ConjunctionImpl(
      List<Var> vars,
      List<UExpr> tables,
      List<UExpr> predicates,
      Disjunction squash,
      Disjunction negation) {
    this.vars = new ArrayList<>(vars);
    this.predicates = predicates;
    this.negation = negation;
    this.squash = squash;
    this.tables = tables;
  }

  static Conjunction make(
      List<Var> vars,
      List<UExpr> tables,
      List<UExpr> predicates,
      Disjunction squash,
      Disjunction negation) {
    if (vars == null || predicates == null || tables == null) throw new IllegalArgumentException();
    if (predicates.isEmpty() && negation == null && squash == null && tables.isEmpty())
      throw new IllegalArgumentException();
    if (predicates.stream().anyMatch(it -> !it.kind().isPred()))
      throw new IllegalArgumentException();
    if (tables.stream().anyMatch(it -> it.kind() != TABLE)) throw new IllegalArgumentException();

    return new ConjunctionImpl(vars, tables, predicates, squash, negation);
  }

  static Conjunction empty() {
    return EMPTY;
  }

  @Override
  public List<Var> vars() {
    return vars;
  }

  @Override
  public List<UExpr> preds() {
    return predicates;
  }

  @Override
  public List<UExpr> tables() {
    return tables;
  }

  @Override
  public Disjunction neg() {
    return negation;
  }

  @Override
  public Disjunction squash() {
    return squash;
  }

  @Override
  public boolean uses(Var v) {
    return !vars.contains(v) && any(tables, it -> it.uses(v))
        || any(predicates, it -> it.uses(v))
        || (squash != null && squash.uses(v))
        || (negation != null && negation.uses(v));
  }

  @Override
  public void subst(Var v1, Var v2) {
    if (v1.equals(v2)) return;
    if (vars.contains(v2)) {
      vars.removeAll(Collections.singleton(v1));
    } else {
      final ListIterator<Var> iter = vars.listIterator();
      while (iter.hasNext()) iter.set(iter.next().subst(v1, v2));
    }

    predicates.forEach(it -> it.subst(v1, v2));
    tables.forEach(it -> it.subst(v1, v2));
    if (negation != null) negation.subst(v1, v2);
    if (squash != null) squash.subst(v1, v2);
  }

  @Override
  public UExpr toExpr() {
    final UExpr factor0 =
        listJoin(predicates, tables).stream().map(UExpr::copy).reduce(UExpr::mul).orElse(null);
    final UExpr factor1 = negation == null ? null : UExpr.not(negation.toExpr());
    final UExpr factor2 = squash == null ? null : UExpr.squash(squash.toExpr());

    UExpr result = factor0;
    result = result == null ? factor1 : factor1 == null ? result : mul(result, factor1);
    result = result == null ? factor2 : factor2 == null ? result : mul(result, factor2);

    if (result == null) throw new IllegalStateException();

    if (!vars.isEmpty()) return sum(vars, result);
    else return result;
  }

  @Override
  public Conjunction copy() {
    return new ConjunctionImpl(
        vars,
        listMap(tables, UExpr::copy),
        listMap(predicates, UExpr::copy),
        squash == null ? null : squash.copy(),
        negation == null ? null : negation.copy());
  }

  @Override
  public String toString() {
    return toExpr().toString();
    //    final StringBuilder builder = new StringBuilder();
    //    if (withSum) builder.append("sum(");
    //    if (!tables.isEmpty()) builder.append(joining(" * ", tables)).append(" * ");
    //    if (!predicates.isEmpty()) builder.append(joining(" * ", predicates)).append(" * ");
    //    if (negation != null) builder.append(negation).append(" * ");
    //    if (squash != null) builder.append(squash).append(" * ");
    //    builder.delete(builder.length() - 3, builder.length());
    //    if (withSum) builder.append(')');
    //    return builder.toString();
  }
}
