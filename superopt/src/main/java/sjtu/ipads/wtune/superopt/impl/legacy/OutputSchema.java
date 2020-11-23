package sjtu.ipads.wtune.superopt.impl.legacy;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Agg;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.union;

public class OutputSchema {
  private final Set<Set<ColumnSet>> possibilities;

  private OutputSchema(Set<Set<ColumnSet>> possibilities) {
    this.possibilities = possibilities;
  }

  public static OutputSchema fromOp(Operator op) {
    return new OutputSchema(
        Collections.singleton(Collections.singleton(ColumnSet.create(op))));
  }

  public static OutputSchema fromJoin(OutputSchema left, OutputSchema right) {
    final Set<Set<ColumnSet>> sets =
        new HashSet<>(left.possibilities.size() * right.possibilities.size());

    for (List<Set<ColumnSet>> lists :
        Sets.cartesianProduct(left.possibilities, right.possibilities))
      sets.add(union(lists.get(0), lists.get(1)));

    return new OutputSchema(sets);
  }

  public static OutputSchema fromProj(OutputSchema columns) {
    final Set<Set<ColumnSet>> sets = new HashSet<>();

    for (Set<ColumnSet> possibility : columns.possibilities)
      for (Set<ColumnSet> subset : Sets.powerSet(possibility))
        if (!subset.isEmpty()) sets.add(subset);

    return new OutputSchema(sets);
  }

  public static OutputSchema fromAgg(Agg agg, OutputSchema columns) {
    final OutputSchema keyCols = fromProj(columns);
    final OutputSchema aggCols = fromOp(agg);
    return fromJoin(keyCols, aggCols);
  }

  public Set<Set<ColumnSet>> possibilities() {
    return possibilities;
  }
}
