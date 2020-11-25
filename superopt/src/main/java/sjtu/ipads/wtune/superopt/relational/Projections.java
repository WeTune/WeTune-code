package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretable;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.impl.ProjectionsImpl;

public interface Projections {
  ColumnSet columns();

  // currently Projections is just an wrapper of SymbolicColumns
  static Projections selectAll(Operator context, Abstraction<InputSource> from) {
    return ProjectionsImpl.create(ColumnSet.nativeColumns(context, from));
  }

  static Projections from(ColumnSet columns) {
    return ProjectionsImpl.create(columns);
  }
}
