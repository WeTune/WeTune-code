package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.PlanImpl;
import sjtu.ipads.wtune.superopt.util.Hole;
import sjtu.ipads.wtune.superopt.util.Lockable;
import sjtu.ipads.wtune.symsolver.core.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public interface Plan extends Comparable<Plan>, Lockable {
  void setId(int i);

  int id();

  PlanNode head();

  void setHead(PlanNode head);

  Plan setup();

  QueryBuilder semantic();

  @Override
  default int compareTo(Plan o) {
    final PlanNode head = this.head(), otherHead = o.head();
    if (head == null && otherHead == null) return 0;
    if (head == null /* && otherHead != null */) return -1;
    if (/* head != null && */ otherHead == null) return 1;

    return head.compareTo(otherHead);
  }

  default String toInformativeString() {
    return stringify(this, true);
  }

  default Plan copy() {
    if (head() == null) return empty();
    else return wrap(head().copy());
  }

  default List<Hole<PlanNode>> holes() {
    if (head() == null) return singletonList(Hole.ofSetter(this::setHead));

    final List<Hole<PlanNode>> holes = new ArrayList<>();
    acceptVisitor(PlanVisitor.traverse(x -> holes.addAll(x.holes())));
    return holes;
  }

  default void acceptVisitor(PlanVisitor visitor) {
    head().acceptVisitor(visitor);
  }

  static Plan empty() {
    return PlanImpl.build();
  }

  static Plan wrap(PlanNode head) {
    final Plan g = Plan.empty();
    g.setHead(head);
    return g;
  }

  static Plan rebuild(String str) {
    return PlanImpl.build(str);
  }
}
