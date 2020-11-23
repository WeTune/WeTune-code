package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.impl.ProjectionsImpl;

public interface Projections {
  SymbolicColumns columns();
  // currently Projections is just an wrapper of SymbolicColumns
  static Projections selectAll(Abstraction<Relation> from) {
    return ProjectionsImpl.create(SymbolicColumns.fromSingle(from));
  }
}
