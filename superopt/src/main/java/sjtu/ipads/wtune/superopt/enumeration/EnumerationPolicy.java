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

  static SymbolicColumns visibleColumns(Interpretation interpretation, Operator op) {
    final SymbolicColumns inputColumns = op.prev()[0].outSchema().columns(interpretation);
    final SymbolicColumns correlatedColumns = findCorrelatedColumns(interpretation, op);

    return correlatedColumns == null
        ? inputColumns
        : SymbolicColumns.concat(inputColumns, correlatedColumns);
  }

  static SymbolicColumns findCorrelatedColumns(Interpretation interpretation, Operator op) {
    final Operator next = op.next();
    final SymbolicColumns outerCols =
        next == null ? null : findCorrelatedColumns(interpretation, next);
    if (op instanceof SubqueryFilter) {
      final SymbolicColumns cols = op.prev()[0].outSchema().columns(interpretation);
      return outerCols == null ? cols : SymbolicColumns.concat(outerCols, cols);

    } else return outerCols;
  }
}
