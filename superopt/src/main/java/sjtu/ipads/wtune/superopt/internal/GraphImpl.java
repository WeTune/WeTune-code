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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public class GraphImpl implements Graph {
  public Operator head;
  public List<Input> inputs;

  private boolean alreadySetup;
  private Semantic semantic;

  private final Lock l;

  private GraphImpl() {
    l = new ReentrantLock();
  }

  public static Graph build() {
    return new GraphImpl();
  }

  public static Graph build(String str) {
    final String[] opStrs = str.split("[(),]+");
    final Deque<Operator> operators = new ArrayDeque<>(opStrs.length);

    for (int i = opStrs.length - 1; i >= 0; i--) {
      final String opStr = opStrs[i];
      final String[] fields = opStr.split("[<> ]+");
      final OperatorType type = OperatorType.valueOf(fields[0]);
      final Operator op = type.create();

      op.setPlaceholders(fields);
      for (int j = 0; j < type.numPredecessors(); j++) op.setPredecessor(j, operators.pop());

      operators.push(op);
    }

    return Graph.wrap(operators.pop()).setup();
  }

  @Override
  public void lock() {
    l.lock();
  }

  @Override
  public void unlock() {
    l.unlock();
  }

  @Override
  public Operator head() {
    return head;
  }

  @Override
  public Graph setup() {
    if (alreadySetup) return this;

    inputs = new ArrayList<>(5);
    int i = 0;
    for (Hole<Operator> hole : holes()) {
      final Input input = Input.create();
      hole.fill(input);
      inputs.add(input);
    }

    acceptVisitor(GraphVisitor.traverse(it -> it.setGraph(this)));

    alreadySetup = true;
    return this;
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
      if (semantic == null) {
        setup();
        semantic = Semantic.build(this);
      }
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
