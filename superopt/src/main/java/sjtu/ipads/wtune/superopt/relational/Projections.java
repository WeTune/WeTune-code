package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.impl.ProjectionsImpl;

public interface Projections {
  enum Range {
    ALL,
    SINGLE,
    SOME,
    SPECIFIC
  }

  static Projections selectAll(Abstraction<Relation> from) {
    return ProjectionsImpl.create(Range.ALL, from);
  }
}
