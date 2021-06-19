package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.*;
import static sjtu.ipads.wtune.prover.expr.UExpr.mul;
import static sjtu.ipads.wtune.prover.expr.UExpr.sum;

final class ConjunctionImpl implements Conjunction {
  private final List<Tuple> sumTuples;
  private final List<UExpr> predicates;
  private final UExpr negation;
  private final UExpr squash;
  private final List<UExpr> tables;

  ConjunctionImpl(
      List<Tuple> sumTuples,
      List<UExpr> tables,
      List<UExpr> predicates,
      UExpr squash,
      UExpr negation) {
    if (sumTuples == null || predicates == null || tables == null)
      throw new IllegalArgumentException();
    if (predicates.isEmpty() && negation == null && squash == null && tables.isEmpty())
      throw new IllegalArgumentException();
    if (negation != null && negation.kind() != NOT) throw new IllegalArgumentException();
    if (squash != null && squash.kind() != SQUASH) throw new IllegalArgumentException();
    if (predicates.stream().anyMatch(it -> !it.kind().isPred()))
      throw new IllegalArgumentException();
    if (tables.stream().anyMatch(it -> it.kind() != TABLE)) throw new IllegalArgumentException();

    this.sumTuples = new ArrayList<>(sumTuples);
    this.predicates = predicates;
    this.negation = negation;
    this.squash = squash;
    this.tables = tables;
  }

  @Override
  public List<Tuple> boundedVars() {
    return sumTuples;
  }

  @Override
  public List<UExpr> predicates() {
    return predicates;
  }

  @Override
  public List<UExpr> tables() {
    return tables;
  }

  @Override
  public UExpr negation() {
    return negation;
  }

  @Override
  public UExpr squash() {
    return squash;
  }

  @Override
  public void subst(Tuple v1, Tuple v2) {
    final ListIterator<Tuple> iter = sumTuples.listIterator();
    while (iter.hasNext()) iter.set(iter.next().subst(v1, v2));

    predicates.forEach(it -> it.subst(v1, v2));
    tables.forEach(it -> it.subst(v1, v2));
    if (negation != null) negation.subst(v1, v2);
    if (squash != null) squash.subst(v1, v2);
  }

  @Override
  public UExpr toExpr() {
    final UExpr factor0 =
        listJoin(predicates, tables).stream().map(UExpr::copy).reduce(UExpr::mul).orElse(null);
    final UExpr factor1 = negation == null ? null : negation.copy();
    final UExpr factor2 = squash == null ? null : squash.copy();

    UExpr result = factor0;
    result = result == null ? factor1 : factor1 == null ? result : mul(result, factor1);
    result = result == null ? factor2 : factor2 == null ? result : mul(result, factor2);

    if (result == null) throw new IllegalStateException();

    if (!sumTuples.isEmpty()) return sum(sumTuples, result);
    else return result;
  }

  @Override
  public Conjunction copy() {
    return new ConjunctionImpl(
        sumTuples,
        listMap(UExpr::copy, tables),
        listMap(UExpr::copy, predicates),
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
