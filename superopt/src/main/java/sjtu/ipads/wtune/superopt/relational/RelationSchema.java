package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.relational.impl.*;

import java.util.Collections;
import java.util.List;

public interface RelationSchema {
  Operator op();

  RelationSchema nonTrivialSource();

  ColumnSet symbolicColumns(Interpretation interpretation);

  List<List<Constraint>> enforceEq(RelationSchema other, Interpretation interpretation);

  boolean shapeEquals(RelationSchema other, Interpretation interpretation);

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

  static RelationSchema create(Union union) {
    return UnionSchema.create(union);
  }

  static RelationSchema create(Operator op) {
    return BaseRelationSchema.create(op);
  }
}
