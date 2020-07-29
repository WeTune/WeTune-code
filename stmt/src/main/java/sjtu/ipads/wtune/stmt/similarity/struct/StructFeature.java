package sjtu.ipads.wtune.stmt.similarity.struct;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StructFeature {
  public int numTables;
  public int numAgg;
  public boolean hasGroupBy;
  public boolean hasOrderBy;
  public boolean hasLimit;
  public boolean hasOffset;
  public boolean hasDistinct;
  public final int[] numOperators = new int[OpCategory.values().length];
  public final Set<StructFeature> subqueries = new HashSet<>();

  public void increaseOperator(OpCategory category) {
    ++numOperators[category.ordinal()];
  }

  public static StructFeature extractFrom(Statement stmt) {
    final StructFeatureVisitor visitor = new StructFeatureVisitor(stmt.parsed());
    stmt.parsed().accept(visitor);
    return visitor.feature();
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StructFeature that = (StructFeature) o;
    return numTables == that.numTables
        && numAgg == that.numAgg
        && hasGroupBy == that.hasGroupBy
        && hasOrderBy == that.hasOrderBy
        && hasLimit == that.hasLimit
        && hasOffset == that.hasOffset
        && hasDistinct == that.hasDistinct
        && Arrays.equals(numOperators, that.numOperators)
        && Objects.equals(subqueries, that.subqueries);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            numTables,
            numAgg,
            hasGroupBy,
            hasOrderBy,
            hasLimit,
            hasOffset,
            hasDistinct,
            subqueries);
    result = 31 * result + Arrays.hashCode(numOperators);
    return result;
  }
}
