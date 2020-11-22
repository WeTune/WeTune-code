package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.relational.Relation;
import sjtu.ipads.wtune.superopt.impl.Hole;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public interface Operator {
  Operator next();

  Operator[] prev();

  boolean setNext(Operator next);

  boolean setPrev(int idx, Operator prev);

  int id();

  void setId(int id);

  Operator copy0();

  RelationSchema outSchema();

  void setOutSchema(RelationSchema schema);

  void accept0(GraphVisitor visitor);

  void leave0(GraphVisitor visitor);

  default boolean canBeQueryOut() {
    return true;
  }

  default Operator copy() {
    final Operator thisCopy = copy0();

    final Operator[] prev = prev();
    for (int i = 0; i < prev.length; i++) {
      if (prev[i] == null) thisCopy.setPrev(i, null);
      else {
        final Operator prevCopy = prev[i].copy();
        thisCopy.setPrev(i, prevCopy);
        prevCopy.setNext(thisCopy);
      }
    }

    return thisCopy;
  }

  default void acceptVisitor(GraphVisitor visitor) {
    if (visitor.enter(this)) {
      accept0(visitor);

      final Operator[] prevs = prev();
      for (int i = 0; i < prevs.length; i++) {
        final Operator prev = prevs[i];
        if (prev != null) prev.acceptVisitor(visitor);
        else visitor.enterEmpty(this, i);
      }

      leave0(visitor);
    }
    visitor.leave(this);
  }

  default List<Hole<Operator>> holes() {
    final Operator[] prev = prev();
    if (prev.length == 1 && prev[0] == null)
      return singletonList(Hole.ofConditionalSetter(x -> setPrev(0, x)));

    final List<Hole<Operator>> ret = new ArrayList<>(prev.length);
    for (int i = 0; i < prev.length; i++) {
      final int j = i;
      if (prev[i] == null) ret.add(Hole.ofConditionalSetter(x -> setPrev(j, x)));
    }

    return ret;
  }

  default int structuralHash() {
    int h = getClass().hashCode();
    for (Operator operator : prev()) {
      if (operator == null) h = h * 31;
      else h = h * 31 + operator.structuralHash();
    }
    return h;
  }

  default boolean structuralEquals(Operator other) {
    if (other == null || other.getClass() != this.getClass()) return false;
    final Operator[] thisPrevs = prev();
    final Operator[] otherPrevs = other.prev();
    for (int i = 0; i < thisPrevs.length; i++) {
      final Operator thisPrev = thisPrevs[i];
      final Operator otherPrev = otherPrevs[i];
      if ((thisPrev == null && otherPrev != null)
          || (thisPrev != null && !thisPrev.structuralEquals(otherPrev))) return false;
    }

    return true;
  }

  static List<Operator> templates() {
    return List.of(
        Agg.create(),
        Distinct.create(),
        Join.create(),
        Limit.create(),
        PlainFilter.create(),
        Proj.create(),
        Sort.create(),
        SubqueryFilter.create(),
        Union.create());
  }
}
