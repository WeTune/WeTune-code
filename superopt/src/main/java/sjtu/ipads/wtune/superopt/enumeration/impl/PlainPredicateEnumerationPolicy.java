package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.PlainPredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.superopt.relational.ColumnSet.selectFrom;

public class PlainPredicateEnumerationPolicy implements EnumerationPolicy<PlainPredicate> {
  private PlainPredicateEnumerationPolicy() {}

  public static PlainPredicateEnumerationPolicy create() {
    return new PlainPredicateEnumerationPolicy();
  }

  @Override
  public Set<PlainPredicate> enumerate(
      Interpretation interpretation, Abstraction<PlainPredicate> target) {
    final Operator context = (Operator) target.interpreter();
    final ColumnSet columns = EnumerationPolicy.visibleColumns(interpretation, context);

    final Set<PlainPredicate> ret = new HashSet<>();
    final List<ColumnSet> selections = new ArrayList<>(selectFrom(columns, 2));

    for (ColumnSet selection : selections) ret.add(PlainPredicate.simple(selection));

    //    for (int i = 0; i < selections.size(); i++)
    //      for (int j = i; j < selections.size(); j++)
    //        ret.add(PlainPredicate.disjunctive(selections.get(i), selections.get(j)));

    return ret;
  }
}
