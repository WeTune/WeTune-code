package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

public interface PlanNode extends Comparable<PlanNode> {
  OperatorType type();

  Plan plan();

  PlanNode[] predecessors();

  void setPlan(Plan plan);

  void setPredecessor(int idx, PlanNode prev);

  void acceptVisitor(PlanVisitor visitor);

  default PlanNode copy() {
    final PlanNode thisCopy = type().create();

    final PlanNode[] prev = predecessors();
    for (int i = 0; i < prev.length; i++) {
      if (prev[i] == null) thisCopy.setPredecessor(i, null);
      else {
        final PlanNode prevCopy = prev[i].copy();
        thisCopy.setPredecessor(i, prevCopy);
      }
    }

    return thisCopy;
  }

  default List<Hole<PlanNode>> holes() {
    final PlanNode[] prev = predecessors();
    final List<Hole<PlanNode>> ret = new ArrayList<>(prev.length);

    for (int i = 0, bound = prev.length; i < bound; i++)
      if (prev[i] == null) {
        final int j = i;
        ret.add(Hole.ofSetter(x -> setPredecessor(j, x)));
      }

    return ret;
  }

  default boolean structuralEquals(PlanNode other) {
    if (other == null || other.getClass() != this.getClass()) return false;
    final PlanNode[] thisPrevs = predecessors();
    final PlanNode[] otherPrevs = other.predecessors();
    for (int i = 0; i < thisPrevs.length; i++) {
      final PlanNode thisPrev = thisPrevs[i];
      final PlanNode otherPrev = otherPrevs[i];
      if ((thisPrev == null && otherPrev != null)
          || (thisPrev != null && !thisPrev.structuralEquals(otherPrev))) return false;
    }

    return true;
  }

  default int structuralHash() {
    int h = getClass().hashCode();
    for (PlanNode planNode : predecessors()) {
      if (planNode == null || planNode instanceof Input) h = h * 31;
      else h = h * 31 + planNode.structuralHash();
    }
    return h;
  }

  @Override
  default int compareTo(PlanNode o) {
    int res = type().compareTo(o.type());
    if (res != 0) return res;

    final PlanNode[] preds = predecessors(), otherPreds = o.predecessors();
    assert preds.length == otherPreds.length;

    for (int i = 0, bound = preds.length; i < bound; i++) {
      final PlanNode pred = preds[i], otherPred = otherPreds[i];
      if (pred == null && otherPred == null) continue;
      if (pred == null /* && otherPred != null */) return -1;
      if (/* pred != null && */ otherPred == null) return 1;

      res = pred.compareTo(otherPred);
      if (res != 0) return res;
    }

    return 0;
  }

  /* static factory methods */
  static Agg agg(PlanNode prev) {
    final Agg op = Agg.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Distinct distinct(PlanNode prev) {
    final Distinct op = Distinct.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Limit limit(PlanNode prev) {
    final Limit op = Limit.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static PlainFilter plainFilter(PlanNode prev) {
    final PlainFilter op = PlainFilter.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Proj proj(PlanNode prev) {
    final Proj op = Proj.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Sort sort(PlanNode prev) {
    final Sort op = Sort.create();
    op.setPredecessor(0, prev);
    return op;
  }

  static Join innerJoin(PlanNode left, PlanNode right) {
    final Join join = InnerJoin.create();
    join.setPredecessor(0, left);
    join.setPredecessor(1, right);
    return join;
  }

  static Join leftJoin(PlanNode left, PlanNode right) {
    final Join join = LeftJoin.create();
    join.setPredecessor(0, left);
    join.setPredecessor(1, right);
    return join;
  }

  static SubqueryFilter subqueryFilter(PlanNode left, PlanNode right) {
    final SubqueryFilter subqueryFilter = SubqueryFilter.create();
    subqueryFilter.setPredecessor(0, left);
    subqueryFilter.setPredecessor(1, right);
    return subqueryFilter;
  }

  static Union union(PlanNode left, PlanNode right) {
    final Union union = Union.create();
    union.setPredecessor(0, left);
    union.setPredecessor(1, right);
    return union;
  }

  static Input input() {
    return Input.create();
  }
}
