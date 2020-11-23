package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.relational.impl.*;

public interface RelationSchema {
  Operator op();

  boolean schemaEquals(RelationSchema other, Interpretation interpretation);

  SymbolicColumns columns(Interpretation interpretation);

  RelationSchema nonTrivialSource();

  // true indicates the output schema is not affected by interpretation
  boolean isStable();

  static RelationSchema create(Agg agg) {
    return AggSchema.create(agg);
  }

  static RelationSchema create(Input input) {
    return InputSchema.create(input);
  }

  static RelationSchema create(Join join) {
    return JoinSchema.create(join);
  }

  static RelationSchema create(Proj proj) {
    return ProjSchema.create(proj);
  }

  static RelationSchema create(Operator op) {
    return BaseRelationSchema.create(op);
  }
}
