package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.SortKeys;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

import static sjtu.ipads.wtune.superopt.relational.ColumnSet.selectFrom;

public class SortKeysEnumerationPolicy extends ColumnEnumerationPolicy<SortKeys> {
  public static SortKeysEnumerationPolicy create() {
    return new SortKeysEnumerationPolicy();
  }

  @Override
  protected Iterable<ColumnSet> selections(ColumnSet source) {
    return selectFrom(source, 2);
  }

  @Override
  protected SortKeys fromSelection(Interpreter interpreter, ColumnSet columns) {
    columns.setInterpreter(interpreter);
    return SortKeys.from(columns);
  }
}
