package sjtu.ipads.wtune.superopt.relational;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.impl.MultiSourceColumnSet;
import sjtu.ipads.wtune.superopt.relational.impl.NativeColumns;
import sjtu.ipads.wtune.superopt.relational.impl.SynthesizedColumns;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ColumnSet {
  enum Range {
    ALL,
    SINGLE,
    SOME,
    SPECIFIC
  }

  Set<MonoSourceColumnSet> flatten();

  ColumnSet copy();

  ColumnSet union(ColumnSet other);

  List<List<Constraint>> enforceEq(ColumnSet other, Interpretation interpretation);

  void setInterpreter(Interpreter interpreter);

  static Set<ColumnSet> selectFrom(ColumnSet set) {
    return selectFrom(set, Integer.MAX_VALUE);
  }

  static Set<ColumnSet> selectFrom(ColumnSet set, int max) {
    final Set<ColumnSet> ret = new HashSet<>();
    final Set<MonoSourceColumnSet> columns = set.flatten();
    for (Set<MonoSourceColumnSet> subset : Sets.powerSet(columns))
      if (!subset.isEmpty() && subset.size() <= max) ret.add(MultiSourceColumnSet.copyFrom(subset));
    return ret;
  }

  static ColumnSet nativeColumns(Interpreter interpreter, Abstraction<InputSource> source) {
    return NativeColumns.create(interpreter, source);
  }

  static ColumnSet union(ColumnSet left, ColumnSet right) {
    return left.union(right);
  }

  static ColumnSet mask(Interpreter interpreter, ColumnSet... src) {
    return SynthesizedColumns.from(interpreter, src);
  }
}
