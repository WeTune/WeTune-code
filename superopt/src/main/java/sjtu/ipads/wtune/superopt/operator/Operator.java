package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

public interface Operator extends Comparable<Operator> {
  OperatorType type();

  Graph graph();

  Operator[] predecessors();

  void setGraph(Graph graph);

  void setPredecessor(int idx, Operator prev);

  boolean accept0(GraphVisitor visitor);

  void leave0(GraphVisitor visitor);

  default void acceptVisitor(GraphVisitor visitor) {
    if (visitor.enter(this)) {
      if (accept0(visitor)) {

        final Operator[] prevs = predecessors();
        for (int i = 0; i < prevs.length; i++) {
          final Operator prev = prevs[i];
          if (prev != null) prev.acceptVisitor(visitor);
          else visitor.enterEmpty(this, i);
        }
      }

      leave0(visitor);
    }
    visitor.leave(this);
  }

  default void setPlaceholders(String[] str) {}

  default Operator copy() {
    final Operator thisCopy = type().create();

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

  @Override
  default int compareTo(Operator o) {
    int res = type().compareTo(o.type());
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
  /* static factory methods */
  static Agg agg(Operator prev) {
    final Agg op = Agg.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Distinct distinct(Operator prev) {
    final Distinct op = Distinct.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Limit limit(Operator prev) {
    final Limit op = Limit.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static PlainFilter plainFilter(Operator prev) {
    final PlainFilter op = PlainFilter.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Proj proj(Operator prev) {
    final Proj op = Proj.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Sort sort(Operator prev) {
    final Sort op = Sort.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Join innerJoin(Operator left, Operator right) {
    final Join join = InnerJoin.create();
    join.setPredecessor(0, left);
    join.setPredecessor(1, right);
    return join;
  }

  static Join leftJoin(Operator left, Operator right) {
    final Join join = LeftJoin.create();
    join.setPredecessor(0, left);
    join.setPredecessor(1, right);
    return join;
  }

  static SubqueryFilter subqueryFilter(Operator left, Operator right) {
    final SubqueryFilter subqueryFilter = SubqueryFilter.create();
    subqueryFilter.setPredecessor(0, left);
    subqueryFilter.setPredecessor(1, right);
    return subqueryFilter;
  }

  static Union union(Operator left, Operator right) {
    final Union union = Union.create();
    union.setPredecessor(0, left);
    union.setPredecessor(1, right);
    return union;
  }

  static Input input() {
    return Input.create();
  }
}