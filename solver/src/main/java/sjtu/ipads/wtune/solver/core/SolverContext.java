package sjtu.ipads.wtune.solver.core;

import sjtu.ipads.wtune.solver.core.impl.Z3SolverContext;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.schema.Schema;

import java.util.Arrays;
import java.util.List;

public interface SolverContext {
  SolverContext register(Schema schema);

  boolean checkUnique(AlgNode q0, AlgNode q1);

  boolean checkOrder(AlgNode q0, AlgNode q1);

  boolean checkEquivalence(AlgNode q0, AlgNode q1);

  Variable const_(DataType dataType, Object value);

  Variable ite(Constraint cond, Variable v0, Variable v1);

  Constraint ite(Constraint cond, Constraint v0, Constraint v1);

  Constraint implies(Constraint c0, Constraint c1);

  Constraint boolConst(boolean value);

  Constraint and(Iterable<Constraint> constraints);

  Constraint or(Iterable<Constraint> constraints);

  Constraint eq(Variable left, Variable right);

  Constraint eq(Constraint left, Constraint right);

  Constraint convert(Variable variable);

  List<SymbolicColumnRef> symbolicColumnsOf(AlgNode node);

  boolean inferFixedValue(SymbolicColumnRef col);

  boolean inferColumnEqs(SymbolicColumnRef c0, SymbolicColumnRef c1);

  default Constraint and(Constraint... constraints) {
    return and(Arrays.asList(constraints));
  }

  default Constraint or(Constraint... constraints) {
    return or(Arrays.asList(constraints));
  }

  static SolverContext z3() {
    return Z3SolverContext.create();
  }
}
