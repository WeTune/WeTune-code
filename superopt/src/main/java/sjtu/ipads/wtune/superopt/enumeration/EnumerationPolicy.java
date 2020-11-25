package sjtu.ipads.wtune.superopt.enumeration;

import sjtu.ipads.wtune.superopt.enumeration.impl.*;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.SubqueryFilter;
import sjtu.ipads.wtune.superopt.relational.*;

import java.util.Set;

public interface EnumerationPolicy<T> {
  Set<T> enumerate(Interpretation interpretation, Abstraction<T> target);

  static EnumerationPolicy<Projections> projectionPolicy() {
    return ProjectionsEnumerationPolicy.create();
  }

  static EnumerationPolicy<SortKeys> sortKeysPolicy() {
    return SortKeysEnumerationPolicy.create();
  }

  static EnumerationPolicy<GroupKeys> groupKeysPolicy() {
    return GroupKeysEnumerationPolicy.create();
  }

  static EnumerationPolicy<PlainPredicate> plainPredicatePolicy() {
    return PlainPredicateEnumerationPolicy.create();
  }

  static EnumerationPolicy<SubqueryPredicate> subqueryPredicatePolicy() {
    return SubqueryPredicateEnumerationPolicy.create();
  }

  static ColumnSet visibleColumns(Interpretation interpretation, Operator op) {
    final ColumnSet inputColumns = op.prev()[0].outSchema().symbolicColumns(interpretation);
    final ColumnSet correlatedColumns = findCorrelatedColumns(interpretation, op);

    return correlatedColumns == null
        ? inputColumns
        : ColumnSet.union(inputColumns, correlatedColumns);
  }

  static ColumnSet findCorrelatedColumns(Interpretation interpretation, Operator op) {
    final Operator next = op.next();
    final ColumnSet outerCols =
        next == null ? null : findCorrelatedColumns(interpretation, next);
    if (op instanceof SubqueryFilter) {
      final ColumnSet cols = op.prev()[0].outSchema().symbolicColumns(interpretation);
      return outerCols == null ? cols : ColumnSet.union(outerCols, cols);

    } else return outerCols;
  }
}
