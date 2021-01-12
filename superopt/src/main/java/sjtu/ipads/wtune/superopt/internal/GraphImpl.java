package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.operator.Input;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.OperatorType;
import sjtu.ipads.wtune.superopt.solving.Semantic;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public class GraphImpl implements Graph {
  public Operator head;
  public List<? extends Input> inputs;

  private Semantic semantic;

  private GraphImpl() {}

  public static Graph build() {
    return new GraphImpl();
  }

  public static Graph build(String str) {
    final String[] opNames = str.split("[(),]+");
    final Deque<Operator> operators = new ArrayDeque<>(opNames.length);

    for (int i = opNames.length - 1; i >= 0; i--) {
      final OperatorType type = OperatorType.valueOf(opNames[i]);
      final Operator op = type.create();
      for (int j = 0; j < type.numPredecessors(); j++) op.setPredecessor(j, operators.pop());
      operators.push(op);
    }

    return Graph.wrap(operators.pop()).setup();
  }

  @Override
  public Operator head() {
    return head;
  }

  @Override
  public List<? extends Input> inputs() {
    if (this.inputs == null) {
      final List<Hole<Operator>> holes = holes();
      final List<Input> inputs = new ArrayList<>(5);
      for (int i = 0, bound = holes.size(); i < bound; i++) {
        final Hole<Operator> hole = holes.get(i);
        final Input input = Input.create();
        input.setIndex(i);
        inputs.add(input);
        hole.fill(input);
      }
      this.inputs = inputs;
    }

    return this.inputs;
  }

  @Override
  public void setHead(Operator head) {
    this.head = head;
  }

  @Override
  public String toString() {
    return stringify(this);
  }

  @Override
  public Semantic semantic() {
    if (semantic != null) return semantic;

    synchronized (this) {
      if (semantic == null) semantic = Semantic.build(this);
      return semantic;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Graph)) return false;
    Graph graph = (Graph) o;
    if ((this.head == null) != (graph.head() == null)) return false;
    if (this.head == null) return true;
    return this.head.structuralEquals(graph.head());
  }

  @Override
  public int hashCode() {
    return head == null ? 0 : head.structuralHash();
  }
}
