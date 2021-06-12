package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.joining;

final class Conjunction {
  private final boolean withSum;
  private final List<UExpr> predicates;
  private final UExpr negation;
  private final UExpr squash;
  private final List<UExpr> tables;

  Conjunction(
      boolean withSum, List<UExpr> predicates, UExpr negation, UExpr squash, List<UExpr> tables) {
    if (predicates == null || tables == null) throw new IllegalArgumentException();
    if (predicates.isEmpty() && negation == null && squash == null && tables.isEmpty())
      throw new IllegalArgumentException();

    this.withSum = withSum;
    this.predicates = predicates;
    this.negation = negation;
    this.squash = squash;
    this.tables = tables;
  }

  public List<UExpr> predicates() {
    return predicates;
  }

  public List<UExpr> tables() {
    return tables;
  }

  public UExpr negation() {
    return negation;
  }

  public UExpr squash() {
    return squash;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    if (withSum) builder.append("sum(");
    if (!tables.isEmpty()) builder.append(joining(" * ", tables)).append(" * ");
    if (!predicates.isEmpty()) builder.append(joining(" * ", predicates)).append(" * ");
    if (negation != null) builder.append(negation).append(" * ");
    if (squash != null) builder.append(squash).append(" * ");
    builder.delete(builder.length() - 3, builder.length());
    if (withSum) builder.append(')');
    return builder.toString();
  }
}
