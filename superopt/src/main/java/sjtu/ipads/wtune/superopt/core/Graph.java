package sjtu.ipads.wtune.superopt.core;

import sjtu.ipads.wtune.superopt.internal.GraphImpl;
import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.solving.Semantic;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public interface Graph extends Comparable<Graph> {
  Operator head();

  void setHead(Operator head);

  Graph setup();

  Semantic semantic();

  @Override
  default int compareTo(Graph o) {
    final Operator head = this.head(), otherHead = o.head();
    if (head == null && otherHead == null) return 0;
    if (head == null /* && otherHead != null */) return -1;
    if (/* head != null && */ otherHead == null) return 1;

    return head.compareTo(otherHead);
  }

  default String toInformativeString() {
    return stringify(this, true);
  }

  default Graph copy() {
    if (head() == null) return empty();
    else return wrap(head().copy());
  }

  default List<Hole<Operator>> holes() {
    if (head() == null) return singletonList(Hole.ofSetter(this::setHead));

    final List<Hole<Operator>> holes = new ArrayList<>();
    acceptVisitor(GraphVisitor.traverse(x -> holes.addAll(x.holes())));
    return holes;
  }

  default void acceptVisitor(GraphVisitor visitor) {
    head().acceptVisitor(visitor);
  }

  static Graph empty() {
    return GraphImpl.build();
  }

  static Graph wrap(Operator head) {
    final Graph g = Graph.empty();
    g.setHead(head);
    return g;
  }

  static Graph rebuild(String str) {
    return GraphImpl.build(str);
  }
}
