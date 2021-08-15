package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.constraint.Constraint;

import java.util.List;

public interface ConstraintEnumerator {
  List<List<Constraint>> enumerate();

  List<List<Constraint>> results();

  boolean prove(List<Constraint> selectedConstraints);

  boolean prove(int[] selectedConstraints);

  boolean prove(boolean[] selectedConstraints);
}
