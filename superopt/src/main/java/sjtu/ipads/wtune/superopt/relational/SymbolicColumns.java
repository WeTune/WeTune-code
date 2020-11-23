package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.impl.NativeColumns;
import sjtu.ipads.wtune.superopt.relational.impl.SynthesizedColumns;

import java.util.Set;

import static java.util.Collections.singleton;

public interface SymbolicColumns {
  enum Range {
    ALL,
    SINGLE,
    SOME,
    SPECIFIC
  }

  SymbolicColumns concat(SymbolicColumns other);

  Set<SymbolicColumns> selections(int max);

  SymbolicColumns copy();

  default Abstraction<Relation> relation() {
    return null;
  }

  default Set<? extends SymbolicColumns> flatten() {
    return singleton(this);
  }

  default Set<SymbolicColumns> selections() {
    return selections(Integer.MAX_VALUE);
  }

  default Abstraction<ConcreteColumns> abstractions() {
    return null; // TODO
  }

  static SymbolicColumns fromSingle(Interpreter interpreter, Abstraction<Relation> source) {
    return NativeColumns.create(interpreter, source);
  }

  static SymbolicColumns concat(SymbolicColumns left, SymbolicColumns right) {
    return left.concat(right);
  }

  static SymbolicColumns mask(Interpreter interpreter, SymbolicColumns... src) {
    return SynthesizedColumns.from(interpreter, src);
  }
}
