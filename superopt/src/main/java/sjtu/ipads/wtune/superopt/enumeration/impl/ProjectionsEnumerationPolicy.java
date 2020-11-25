package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.Projections;

import static sjtu.ipads.wtune.superopt.relational.ColumnSet.selectFrom;

public class ProjectionsEnumerationPolicy extends ColumnEnumerationPolicy<Projections> {
  public static ProjectionsEnumerationPolicy create() {
    return new ProjectionsEnumerationPolicy();
  }

  @Override
  protected Iterable<ColumnSet> selections(ColumnSet source) {
    return selectFrom(source);
  }

  @Override
  protected Projections fromSelection(Interpreter interpreter, ColumnSet columns) {
    columns.setInterpreter(interpreter);
    return Projections.from(columns);
  }
}
