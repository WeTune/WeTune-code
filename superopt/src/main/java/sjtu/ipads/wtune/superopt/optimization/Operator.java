package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.superopt.plan.OperatorType;

public interface Operator {
  OperatorType type();

  Operator successor();

  Operator[] predecessors();

  void setPredecessor(int idx, Operator op);

  void setSuccessor(Operator successor);

  default void replacePredecessor(Operator target, Operator rep) {
    final Operator[] predecessors = predecessors();
    for (int i = 0; i < predecessors.length; i++)
      if (predecessors[i] == target) {
        setPredecessor(i, rep);
        break;
      }
  }
}
