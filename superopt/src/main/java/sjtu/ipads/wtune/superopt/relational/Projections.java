package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.impl.ProjectionsImpl;

public interface Projections {
  SymbolicColumns columns();
  // currently Projections is just an wrapper of SymbolicColumns
  static Projections selectAll(Operator context, Abstraction<InputSource> from) {
    return ProjectionsImpl.create(SymbolicColumns.fromSingle(context, from));
  }

  static Projections from(SymbolicColumns columns) {
    return ProjectionsImpl.create(columns);
  }
}
