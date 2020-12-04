package sjtu.ipads.wtune.solver.core;

import sjtu.ipads.wtune.solver.core.impl.Z3SolverContext;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.schema.Schema;

import java.util.Arrays;
import java.util.List;

public interface SolverContext {
  boolean checkEquivalence(Schema schema, AlgNode q0, AlgNode q1);

  Variable const_(DataType dataType, Object value);

  Constraint ite(Constraint cond, Constraint v0, Constraint v1);

  Constraint boolConst(boolean value);

  Constraint and(Iterable<Constraint> constraints);

  Constraint or(Iterable<Constraint> constraints);

  Constraint eq(Variable left, Variable right);

  Constraint eq(Constraint left, Constraint right);

  Constraint convert(Variable variable);

  List<SymbolicColumnRef> tupleSourceOf(AlgNode node);

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
