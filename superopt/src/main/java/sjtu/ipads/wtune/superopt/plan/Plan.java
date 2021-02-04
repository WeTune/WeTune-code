package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.plan.internal.PlanImpl;
import sjtu.ipads.wtune.superopt.plan.internal.Semantic;
import sjtu.ipads.wtune.superopt.plan.internal.ToASTTranslator;
import sjtu.ipads.wtune.superopt.util.Hole;
import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public interface Plan extends Comparable<Plan> {
  void setId(int i);

  int id();

  PlanNode head();

  void setHead(PlanNode head);

  // add Input node as leaves & call setPlan(this) on each node
  Plan setup();

  Semantic semantic();

  default Plan copy() {
    if (head() == null) return empty();
    else return wrap(head().copy());
  }

  default ASTNode sql() {
    return ToASTTranslator.translate(this);
  }

  default List<Hole<PlanNode>> holes() {
    if (head() == null) return singletonList(Hole.ofSetter(this::setHead));

    final List<Hole<PlanNode>> holes = new ArrayList<>();
    acceptVisitor(PlanVisitor.traverse(x -> holes.addAll(x.holes())));
    return holes;
  }

  default String toString(PlaceholderNumbering numbering) {
    return stringify(this, numbering);
  }

  default void acceptVisitor(PlanVisitor visitor) {
    head().acceptVisitor(visitor);
  }

  @Override
  default int compareTo(Plan o) {
    final PlanNode head = this.head(), otherHead = o.head();
    if (head == null && otherHead == null) return 0;
    if (head == null /* && otherHead != null */) return -1;
    if (/* head != null && */ otherHead == null) return 1;

    return head.compareTo(otherHead);
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
