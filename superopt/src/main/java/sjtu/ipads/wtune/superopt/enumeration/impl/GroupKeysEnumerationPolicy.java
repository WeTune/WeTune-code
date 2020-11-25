package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

import static sjtu.ipads.wtune.superopt.relational.ColumnSet.selectFrom;

public class GroupKeysEnumerationPolicy extends ColumnEnumerationPolicy<GroupKeys> {
  public static GroupKeysEnumerationPolicy create() {
    return new GroupKeysEnumerationPolicy();
  }

  @Override
  protected Iterable<ColumnSet> selections(ColumnSet source) {
    return selectFrom(source, 2);
  }

  @Override
  protected GroupKeys fromSelection(Interpreter interpreter, ColumnSet columns) {
    columns.setInterpreter(interpreter);
    return GroupKeys.from(columns);
  }
}
