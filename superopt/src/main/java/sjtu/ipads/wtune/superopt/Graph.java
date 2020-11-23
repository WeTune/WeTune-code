package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.impl.GraphImpl;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public interface Graph {
  Operator head();

  void setHead(Operator head);

  Graph copy0();

  List<Input> inputs();

  Graph freeze();

  List<Abstraction<?>> abstractions();

  List<Interpretation> interpretations();

  List<Constraint> constraints();

  default Graph copy() {
    final Graph thisCopy = copy0();
    if (head() != null) thisCopy.setHead(head().copy());
    return thisCopy;
  }

  default void acceptVisitor(GraphVisitor visitor) {
    head().acceptVisitor(visitor);
  }

  default List<Hole<Operator>> holes() {
    if (head() == null) return singletonList(Hole.ofSetter(this::setHead));

    final List<Hole<Operator>> holes = new ArrayList<>();
    acceptVisitor(GraphVisitor.traversal(x -> holes.addAll(x.holes())));
    return holes;
  }

  default boolean isEmpty() {
    return head() == null;
  }

  static Graph createEmpty() {
    return new GraphImpl();
  }
}
