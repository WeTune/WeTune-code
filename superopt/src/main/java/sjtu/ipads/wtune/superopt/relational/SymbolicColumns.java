package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.impl.SingleSourceSymbolicColumns;

import java.util.Set;

public interface SymbolicColumns {
  enum Range {
    ALL,
    SINGLE,
    SOME,
    SPECIFIC
  }

  SymbolicColumns concat(SymbolicColumns other);

  Set<SymbolicColumns> selections();

  default Abstraction<ConcreteColumns> abstractions() {

    return null; // TODO
  }

  static SymbolicColumns fromSingle(Abstraction<Relation> source) {
    return SingleSourceSymbolicColumns.create(source);
  }
}
