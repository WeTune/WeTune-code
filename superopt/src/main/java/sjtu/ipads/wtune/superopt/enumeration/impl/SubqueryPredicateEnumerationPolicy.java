package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.HashSet;
import java.util.Set;

public class SubqueryPredicateEnumerationPolicy implements EnumerationPolicy<SubqueryPredicate> {
  private SubqueryPredicateEnumerationPolicy() {}

  public static SubqueryPredicateEnumerationPolicy create() {
    return new SubqueryPredicateEnumerationPolicy();
  }

  @Override
  public Set<SubqueryPredicate> enumerate(
      Interpretation interpretation, Abstraction<SubqueryPredicate> target) {
    final Operator context = (Operator) target.interpreter();
    final SymbolicColumns columns = EnumerationPolicy.visibleColumns(interpretation, context);

    final Set<SubqueryPredicate> ret = new HashSet<>();
    for (SymbolicColumns selection : columns.selections(1))
      ret.add(SubqueryPredicate.in(selection));

    ret.add(SubqueryPredicate.exists());

    return ret;
  }
}
