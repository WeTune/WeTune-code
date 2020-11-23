package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

public class GraphImpl implements Graph {
  public Operator head;
  private Interpretation interpretation = Interpretation.create();

  public List<Input> inputs;
  private List<Abstraction<?>> abstractions;
  private boolean frozen = false;

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
  public List<Abstraction<?>> abstractions() {
    return abstractions;
  }

  @Override
  public void freeze() {
    if (frozen) return;
    frozen = true;

    int i = 0;
    final List<Input> inputs = new ArrayList<>(5);
    for (Hole<Operator> hole : holes()) {
      final Input input = Input.create(i++);
      inputs.add(input);
      hole.fill(input);
    }
    this.inputs = inputs;

    this.abstractions = AbstractionCollector.collect(this);

    acceptVisitor(new IdMarker());
    acceptVisitor(new SchemaMarker());
    acceptVisitor(new UnionSchemaMarker());
  }

  @Override
  public Interpretation interpretation() {
    return interpretation;
  }

  @Override
  public void setInterpretation(Interpretation interpretation) {
    this.interpretation = interpretation;
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

  private static class IdMarker implements GraphVisitor {
    private int nextId = 1;

    @Override
    public boolean enter(Operator op) {
      op.setId(nextId++);
      return true;
    }
  }

  private static class SchemaMarker implements GraphVisitor {
    @Override
    public void leave(Operator op) {
      final RelationSchema schema;

      if (op instanceof Agg) schema = RelationSchema.create((Agg) op);
      else if (op instanceof Input) schema = RelationSchema.create((Input) op);
      else if (op instanceof Join) schema = RelationSchema.create((Join) op);
      else if (op instanceof Proj) schema = RelationSchema.create((Proj) op);
      else schema = RelationSchema.create(op);

      op.setOutSchema(schema);
    }
  }

  private class UnionSchemaMarker implements GraphVisitor {
    @Override
    public boolean enterUnion(Union op) {
      final Constraint constraint =
          Constraint.schemaEq(op.prev()[0].outSchema(), op.prev()[1].outSchema());
      interpretation.addConstraint(constraint);

      return true;
    }
  }

  private static class AbstractionCollector implements GraphVisitor {
    private final List<Abstraction<?>> abstractions = new ArrayList<>();

    @Override
    public boolean enterAgg(Agg op) {
      abstractions.add(op.aggFuncs());
      abstractions.add(op.groupKeys());
      return true;
    }

    @Override
    public boolean enterInput(Input input) {
      abstractions.add(input.relation());
      return true;
    }

    @Override
    public boolean enterProj(Proj op) {
      abstractions.add(op.projs());
      return true;
    }

    @Override
    public boolean enterPlainFilter(PlainFilter op) {
      abstractions.add(op.predicate());
      return true;
    }

    @Override
    public boolean enterSubqueryFilter(SubqueryFilter op) {
      abstractions.add(op.predicate());
      return true;
    }

    @Override
    public boolean enterSort(Sort op) {
      abstractions.add(op.sortKeys());
      return true;
    }

    public static List<Abstraction<?>> collect(Graph graph) {
      final AbstractionCollector collector = new AbstractionCollector();
      graph.acceptVisitor(collector);
      return collector.abstractions;
    }
  }
}
