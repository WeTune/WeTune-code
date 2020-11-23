package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.PlainPredicate;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlainPredicateEnumerationPolicy implements EnumerationPolicy<PlainPredicate> {
  private PlainPredicateEnumerationPolicy() {}

  public static PlainPredicateEnumerationPolicy create() {
    return new PlainPredicateEnumerationPolicy();
  }

  @Override
  public Set<PlainPredicate> enumerate(
      Interpretation interpretation, Abstraction<PlainPredicate> target) {
    final Operator context = (Operator) target.interpreter();
    final SymbolicColumns columns = EnumerationPolicy.visibleColumns(interpretation, context);

    final Set<PlainPredicate> ret = new HashSet<>();
    final List<SymbolicColumns> selections = new ArrayList<>(columns.selections(2));

    for (SymbolicColumns selection : selections) ret.add(PlainPredicate.simple(selection));

    //    for (int i = 0; i < selections.size(); i++)
    //      for (int j = i; j < selections.size(); j++)
    //        ret.add(PlainPredicate.disjunctive(selections.get(i), selections.get(j)));

    return ret;
  }
}
