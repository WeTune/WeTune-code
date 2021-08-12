package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.internal.FragmentImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholders;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public interface Fragment extends Comparable<Fragment> {
  void setId(int i);

  int id();

  Operator head();

  void setHead(Operator head);

  // add Input node as leaves & call setPlan(this) on each node
  Fragment setup();

  Semantic semantic();

  Placeholders placeholders();

  default Fragment copy() {
    if (head() == null) return empty();
    else return wrap(head().copy());
  }

  default List<Hole<Operator>> holes() {
    if (head() == null) return singletonList(Hole.ofSetter(this::setHead));

    final List<Hole<Operator>> holes = new ArrayList<>();
    acceptVisitor(OperatorVisitor.traverse(x -> holes.addAll(x.holes())));
    return holes;
  }

  default String toString(Numbering numbering) {
    return stringify(this, numbering);
  }

  default void acceptVisitor(OperatorVisitor visitor) {
    head().acceptVisitor(visitor);
  }

  default PlanNode instantiate(Interpretations interpretations) {
    return head().instantiate(interpretations);
  }

  @Override
  default int compareTo(Fragment o) {
    final Operator head = this.head(), otherHead = o.head();
    if (head == null && otherHead == null) return 0;
    if (head == null /* && otherHead != null */) return -1;
    if (/* head != null && */ otherHead == null) return 1;

    return head.compareTo(otherHead);
  }

  static Fragment empty() {
    return FragmentImpl.build();
  }

  static Fragment wrap(Operator head) {
    final Fragment g = Fragment.empty();
    g.setHead(head);
    return g;
  }

  static Fragment rebuild(String str) {
    return FragmentImpl.build(str);
  }
}
