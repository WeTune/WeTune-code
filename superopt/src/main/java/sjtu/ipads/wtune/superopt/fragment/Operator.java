package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.common.utils.TypedTreeNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

public interface Operator extends TypedTreeNode<OperatorType>, Comparable<Operator> {
  Fragment fragment();

  Operator successor();

  Operator[] predecessors();

  void setFragment(Fragment fragment);

  void setSuccessor(Operator node);

  void setPredecessor(int idx, Operator prev);

  void acceptVisitor(OperatorVisitor visitor);

  default Operator copy() {
    final Operator thisCopy = Operators.create(kind());

    final Operator[] prev = predecessors();
    for (int i = 0; i < prev.length; i++) {
      if (prev[i] == null) thisCopy.setPredecessor(i, null);
      else {
        final Operator prevCopy = prev[i].copy();
        thisCopy.setPredecessor(i, prevCopy);
      }
    }

    return thisCopy;
  }

  default List<Hole<Operator>> holes() {
    final Operator[] prev = predecessors();
    final List<Hole<Operator>> ret = new ArrayList<>(prev.length);

    for (int i = 0, bound = prev.length; i < bound; i++)
      if (prev[i] == null) {
        final int j = i;
        ret.add(Hole.ofSetter(x -> setPredecessor(j, x)));
      }

    return ret;
  }

  default void replacePredecessor(Operator target, Operator rep) {
    final Operator[] pres = predecessors();
    for (int i = 0; i < pres.length; i++)
      if (pres[i] == target) {
        setPredecessor(i, rep);
        break;
      }
  }

  default boolean structuralEquals(Operator other) {
    if (other == null || other.getClass() != this.getClass()) return false;
    final Operator[] thisPrevs = predecessors();
    final Operator[] otherPrevs = other.predecessors();
    for (int i = 0; i < thisPrevs.length; i++) {
      final Operator thisPrev = thisPrevs[i];
      final Operator otherPrev = otherPrevs[i];
      if ((thisPrev == null && otherPrev != null)
          || (thisPrev != null && !thisPrev.structuralEquals(otherPrev))) return false;
    }

    return true;
  }

  default int structuralHash() {
    int h = getClass().hashCode();
    for (Operator operator : predecessors()) {
      if (operator == null || operator instanceof Input) h = h * 31;
      else h = h * 31 + operator.structuralHash();
    }
    return h;
  }

  default boolean match(PlanNode node, Interpretations m) {
    throw new UnsupportedOperationException();
  }

  default PlanNode instantiate(Interpretations interpretations) {
    throw new UnsupportedOperationException();
  }

  @Override
  default int compareTo(Operator o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final Operator[] preds = predecessors(), otherPreds = o.predecessors();
    assert preds.length == otherPreds.length;

    for (int i = 0, bound = preds.length; i < bound; i++) {
      final Operator pred = preds[i], otherPred = otherPreds[i];
      if (pred == null && otherPred == null) continue;
      if (pred == null /* && otherPred != null */) return -1;
      if (/* pred != null && */ otherPred == null) return 1;

      res = pred.compareTo(otherPred);
      if (res != 0) return res;
    }

    return 0;
  }
}
