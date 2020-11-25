package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.superopt.relational.ColumnSet.selectFrom;

public class SubqueryPredicateEnumerationPolicy implements EnumerationPolicy<SubqueryPredicate> {
  private SubqueryPredicateEnumerationPolicy() {}

  public static SubqueryPredicateEnumerationPolicy create() {
    return new SubqueryPredicateEnumerationPolicy();
  }

  @Override
  public Set<SubqueryPredicate> enumerate(
      Interpretation interpretation, Abstraction<SubqueryPredicate> target) {
    final Operator context = (Operator) target.interpreter();
    final ColumnSet columns = EnumerationPolicy.visibleColumns(interpretation, context);

    final Set<SubqueryPredicate> ret = new HashSet<>();
    for (ColumnSet selection : selectFrom(columns, 1)) ret.add(SubqueryPredicate.in(selection));

    ret.add(SubqueryPredicate.exists());

    return ret;
  }
}
