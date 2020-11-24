package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public abstract class ColumnEnumerationPolicy<T> implements EnumerationPolicy<T> {
  //  private final Map<SymbolicColumns, Set<T>> cache = new IdentityHashMap<>();

  private Set<T> getCache(Interpreter interpreter, SymbolicColumns source) {
    final Set<T> keys = new HashSet<>();

    for (SymbolicColumns selection : selections(source))
      keys.add(fromSelection(interpreter, selection));

    //    cache.put(source, keys);
    return keys;
  }

  @Override
  public Set<T> enumerate(Interpretation interpretation, Abstraction<T> target) {
    final Operator op = (Operator) target.interpreter();
    final SymbolicColumns sourceCols = op.prev()[0].outSchema().columns(interpretation);
    return getCache(op, sourceCols);
  }

  protected abstract Iterable<SymbolicColumns> selections(SymbolicColumns source);

  protected abstract T fromSelection(Interpreter interpreter, SymbolicColumns columns);
}
