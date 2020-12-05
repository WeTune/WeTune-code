package sjtu.ipads.wtune.solver.core.impl;

import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.core.Variable;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

public class SymbolicColumnRefImpl implements SymbolicColumnRef {
  private Variable variable;
  private Constraint notNull;
  private Constraint condition;

  private ColumnRef ref;

  private SymbolicColumnRefImpl(Variable variable, Constraint notNull, Constraint condition) {
    this.variable = variable;
    this.condition = condition;
    this.notNull = notNull;
  }

  public static SymbolicColumnRefImpl create(
      Variable variable, Constraint notNull, Constraint condition) {
    return new SymbolicColumnRefImpl(variable, notNull, condition);
  }

  @Override
  public Variable variable() {
    return variable;
  }

  @Override
  public Constraint condition() {
    return condition;
  }

  @Override
  public Constraint notNull() {
    return notNull;
  }

  @Override
  public SymbolicColumnRef setColumnRef(ColumnRef ref) {
    this.ref = ref;
    return this;
  }

  @Override
  public SymbolicColumnRef setVariable(Variable variable) {
    this.variable = variable;
    return this;
  }

  @Override
  public SymbolicColumnRef setNotNull(Constraint notNull) {
    this.notNull = notNull;
    return this;
  }

  @Override
  public SymbolicColumnRef setCondition(Constraint condition) {
    this.condition = condition;
    return this;
  }

  @Override
  public ColumnRef columnRef() {
    return ref;
  }

  @Override
  public String toString() {
    return "sym:" + (ref == null ? "?" : ref.toString());
  }
}
