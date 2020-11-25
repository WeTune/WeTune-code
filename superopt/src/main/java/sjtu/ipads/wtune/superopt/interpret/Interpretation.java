package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.impl.InterpretationImpl;
import sjtu.ipads.wtune.superopt.interpret.impl.MergedInterpretation;
import sjtu.ipads.wtune.superopt.relational.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Interpretation {
  <T> T interpret(Abstraction<T> abs);

  boolean assign(Abstraction<?> abs, Object interpretation);

  boolean assignEq(ConstraintSet constraints, boolean alsoCheck);

  Interpretation assignNew(Abstraction<?> abs, Object interpretation);

  Set<Abstraction<?>> abstractions();

  Collection<Object> assignments();

  default boolean assignEq(ConstraintSet constraints) {
    return assignEq(constraints, false);
  }

  static Interpretation create() {
    return InterpretationImpl.create();
  }

  static Interpretation merge(Interpretation left, Interpretation right) {
    return MergedInterpretation.create(left, right);
  }

  static List<MonoSourceColumnSet> collectColumns(Interpretation interpretation) {
    final Set<Abstraction<?>> abstractions = interpretation.abstractions();
    final List<MonoSourceColumnSet> columns = new ArrayList<>();

    for (Abstraction<?> abstraction : abstractions) {
      final Object assignment = interpretation.interpret(abstraction);
      if (assignment instanceof SubqueryPredicate)
        // memo: ExistSubqueryPredicate.columns() returns null
        addSymbolicColumns(columns, ((SubqueryPredicate) assignment).columns());
      else if (assignment instanceof PlainPredicate)
        addSymbolicColumns(columns, ((PlainPredicate) assignment).columns());
      else if (assignment instanceof Projections)
        addSymbolicColumns(columns, ((Projections) assignment).columns());
    }

    return columns;
  }

  private static void addSymbolicColumns(List<MonoSourceColumnSet> columns, ColumnSet c) {
    if (c != null) columns.addAll(c.flatten());
  }

  private static void addSymbolicColumns(
      List<MonoSourceColumnSet> columns, Collection<ColumnSet> toAdd) {
    for (ColumnSet c : toAdd) addSymbolicColumns(columns, c);
  }
}
