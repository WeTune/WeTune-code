package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

import java.util.HashSet;
import java.util.Set;

public abstract class ColumnEnumerationPolicy<T> implements EnumerationPolicy<T> {

  @Override
  public Set<T> enumerate(Interpretation interpretation, Abstraction<T> target) {
    final Operator op = (Operator) target.interpreter();
    final ColumnSet sourceCols = op.prev()[0].outSchema().symbolicColumns(interpretation);

    final Set<T> keys = new HashSet<>();
    for (ColumnSet selection : selections(sourceCols)) keys.add(fromSelection(op, selection));

    return keys;
  }

  protected abstract Iterable<ColumnSet> selections(ColumnSet source);

  protected abstract T fromSelection(Interpreter interpreter, ColumnSet columns);
}
