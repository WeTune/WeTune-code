package sjtu.ipads.wtune.solver.core;

import sjtu.ipads.wtune.solver.core.impl.SymbolicColumnRefImpl;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface SymbolicColumnRef {
  Variable variable();

  Constraint condition();

  Constraint notNull();

  ColumnRef columnRef();

  SymbolicColumnRef setColumnRef(ColumnRef ref);

  SymbolicColumnRef setVariable(Variable variable);

  SymbolicColumnRef setNotNull(Constraint notNull);

  SymbolicColumnRef setCondition(Constraint condition);

  default SymbolicColumnRef updateVariable(BiFunction<Constraint, Variable, Variable> combiner) {
    return setVariable(combiner.apply(condition(), variable()));
  }

  default SymbolicColumnRef updateCondition(
      Constraint newCond, BiFunction<Constraint, Constraint, Constraint> combiner) {
    return setCondition(combiner.apply(condition(), newCond));
  }

  default SymbolicColumnRef updateNotNull(
      Constraint newCond, BiFunction<Constraint, Constraint, Constraint> combiner) {
    return setNotNull(combiner.apply(notNull(), newCond));
  }

  default SymbolicColumnRef copy() {
    return create(variable(), notNull(), condition()).setColumnRef(columnRef());
  }

  static SymbolicColumnRef create(Variable variable, Constraint notNull, Constraint condition) {
    return SymbolicColumnRefImpl.create(variable, notNull, condition);
  }

  static SymbolicColumnRef create(Variable variable, Constraint notNull) {
    return SymbolicColumnRefImpl.create(variable, notNull, null);
  }
}
