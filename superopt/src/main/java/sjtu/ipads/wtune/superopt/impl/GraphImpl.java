package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;
import sjtu.ipads.wtune.superopt.relational.SortKeys;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.*;

public class GraphImpl implements Graph {
  public Operator head;

  public List<Input> inputs;
  private List<Abstraction<?>> abstractions;
  private boolean frozen = false;

  private List<Interpretation> interpretations;
  private List<Constraint> constraints;

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
  public List<Constraint> constraints() {
    return constraints;
  }

  @Override
  public List<Interpretation> interpretations() {
    return interpretations;
  }

  @Override
  public Graph freeze() {
    if (frozen) return this;
    frozen = true;

    int i = 0;
    final List<Input> inputs = new ArrayList<>(5);
    for (Hole<Operator> hole : holes()) {
      final Input input = Input.create(i++);
      inputs.add(input);
      hole.fill(input);
    }
    this.inputs = inputs;

    acceptVisitor(new IdMarker());
    acceptVisitor(new SchemaMarker());

    this.constraints = UnionSchemaMarker.collect(this);
    if (constraints == null) {
      return null;
    }

    this.abstractions = AbstractionCollector.collect(this);
    this.interpretations = InterpretationEnumerator.enumerate(this);
    return this;
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

  private static class UnionSchemaMarker implements GraphVisitor {
    private List<Constraint> constraints = null;
    private boolean unfeasible = false;

    @Override
    public boolean enterUnion(Union op) {
      final List<Constraint> constraint =
          Constraint.schemaEq(op.prev()[0].outSchema(), op.prev()[1].outSchema());

      if (constraint == null) {
        unfeasible = true;
        return false;
      }

      if (constraints == null) constraints = new ArrayList<>(4);
      constraints.addAll(constraint);
      return true;
    }

    public static List<Constraint> collect(Graph g) {
      final UnionSchemaMarker visitor = new UnionSchemaMarker();
      g.acceptVisitor(visitor);
      return visitor.unfeasible
          ? null
          : visitor.constraints == null ? Collections.emptyList() : visitor.constraints;
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

  public static class InterpretationEnumerator implements GraphVisitor {
    private final List<Interpretation> interpretations = new LinkedList<>();

    private final EnumerationPolicy<Projections> projectionPolicy =
        EnumerationPolicy.projectionPolicy();
    private final EnumerationPolicy<SortKeys> sortKeysPolicy = EnumerationPolicy.sortKeysPolicy();
    private final EnumerationPolicy<GroupKeys> groupKeysPolicy =
        EnumerationPolicy.groupKeysPolicy();

    private InterpretationEnumerator(Collection<Constraint> constraints) {
      final Interpretation i = Interpretation.create();
      i.addConstraints(constraints);
      interpretations.add(i);
    }

    private <T> void enum0(Abstraction<T> abstraction, EnumerationPolicy<T> policy) {
      final ListIterator<Interpretation> iter = interpretations.listIterator();
      while (iter.hasNext()) {
        final Interpretation interpretation = iter.next();
        iter.remove();

        final Set<T> assignments = policy.enumerate(interpretation, abstraction);
        for (T assignment : assignments) {
          final Interpretation newInterpretation =
              interpretation.assignNew(abstraction, assignment);
          if (newInterpretation != null) iter.add(newInterpretation);
        }
      }
    }

    @Override
    public void leaveProj(Proj op) {
      enum0(op.projs(), projectionPolicy);
    }

    @Override
    public void leaveSort(Sort op) {
      enum0(op.sortKeys(), sortKeysPolicy);
    }

    @Override
    public void leaveAgg(Agg op) {
      enum0(op.groupKeys(), groupKeysPolicy);
    }

    public static List<Interpretation> enumerate(Graph g) {
      final InterpretationEnumerator enumerator = new InterpretationEnumerator(g.constraints());
      g.acceptVisitor(enumerator);
      return enumerator.interpretations;
    }
  }
}
