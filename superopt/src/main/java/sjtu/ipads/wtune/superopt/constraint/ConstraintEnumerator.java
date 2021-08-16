package sjtu.ipads.wtune.superopt.constraint;

import java.util.List;

public interface ConstraintEnumerator {
  void close();

  void setTimeout(long millis);

  List<List<Constraint>> enumerate();

  List<List<Constraint>> results();

  boolean prove(List<Constraint> selectedConstraints);

  boolean prove(int[] selectedConstraints);

  boolean prove(boolean[] selectedConstraints);
}
