package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.Hole;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;
import sjtu.ipads.wtune.superopt.operators.Input;

import java.util.ArrayList;
import java.util.List;

public class GraphImpl implements Graph {
  public Operator head;
  public List<Input> inputs;
  private InterpretationContext interpretations = InterpretationContext.empty();

  @Override
  public Operator head() {
    return head;
  }

  @Override
  public void setHead(Operator head) {
    this.head = head;
  }

  @Override
  public Graph copy0() {
    return new GraphImpl();
  }

  @Override
  public List<Input> inputs() {
    return inputs;
  }

  @Override
  public void setupInputs() {
    if (inputs != null) return;

    int i = 0;
    final List<Input> inputs = new ArrayList<>(5);
    for (Hole<Operator> hole : holes()) {
      final Input input = Input.create(i++);
      inputs.add(input);
      hole.fill(input);
    }
    this.inputs = inputs;
  }

  @Override
  public InterpretationContext interpretations() {
    return interpretations;
  }

  @Override
  public InterpretationContext mergeInterpretations(InterpretationContext other) {
    return interpretations = interpretations.merge(other);
  }

  @Override
  public String toString() {
    final Stringifier stringifier = new Stringifier();
    acceptVisitor(stringifier);
    return stringifier.toString();
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
