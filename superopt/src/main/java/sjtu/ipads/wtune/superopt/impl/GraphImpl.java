package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.enumeration.EnumerationPolicy;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.relational.*;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.*;

public class GraphImpl implements Graph {
  public Operator head;

  public List<Input> inputs;
  private List<Abstraction<?>> abstractions;
  private boolean frozen = false;

  private List<Interpretation> interpretations;
  private Collection<Constraint> constraints;

  private String name;

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
  public Collection<Constraint> constraints() {
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

    acceptVisitor(new IdPlacer());
    acceptVisitor(new GraphPlacer());
    acceptVisitor(new InterpreterPlacer());

    this.constraints = UnionSchemaMarker.collect(this);
    this.abstractions = AbstractionCollector.collect(this);
    this.interpretations = InterpretationEnumerator.enumerate(this);
    return this;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
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

  private static class IdPlacer implements GraphVisitor {
    private int nextId = 0;

    @Override
    public void leave(Operator op) {
      op.setId(nextId++);
    }
  }

  private class GraphPlacer implements GraphVisitor {
    @Override
    public boolean enter(Operator op) {
      op.setGraph(GraphImpl.this);
      return true;
    }
  }

  private static class InterpreterPlacer implements GraphVisitor {
    @Override
    public boolean enter(Operator op) {
      op.setInterpreter(Interpreter.global());
      return true;
    }
  }

  private static class UnionSchemaMarker implements GraphVisitor {
    private Set<Constraint> constraints = null;

    @Override
    public boolean enterUnion(Union op) {
      final RelationSchema leftInput = op.prev()[0].outSchema();
      final RelationSchema rightInput = op.prev()[1].outSchema();
      final RelationSchema myOutput = op.outSchema();

      if (constraints == null) constraints = new HashSet<>(4);
      constraints.add(Constraint.schemaShapeEq(leftInput, rightInput));
      constraints.add(Constraint.schemaShapeEq(myOutput, leftInput));
      constraints.add(Constraint.schemaShapeEq(myOutput, rightInput));
      return true;
    }

    public static Set<Constraint> collect(Graph g) {
      final UnionSchemaMarker visitor = new UnionSchemaMarker();
      g.acceptVisitor(visitor);
      return visitor.constraints == null ? Collections.emptySet() : visitor.constraints;
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
      abstractions.add(input.source());
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

  private static class InterpretationEnumerator implements GraphVisitor {
    private final List<Interpretation> interpretations = new LinkedList<>();

    private final EnumerationPolicy<Projections> projectionPolicy =
        EnumerationPolicy.projectionPolicy();
    private final EnumerationPolicy<SortKeys> sortKeysPolicy = EnumerationPolicy.sortKeysPolicy();
    private final EnumerationPolicy<GroupKeys> groupKeysPolicy =
        EnumerationPolicy.groupKeysPolicy();
    private final EnumerationPolicy<PlainPredicate> plainPredicatePolicy =
        EnumerationPolicy.plainPredicatePolicy();
    private final EnumerationPolicy<SubqueryPredicate> subqueryPredicatePolicy =
        EnumerationPolicy.subqueryPredicatePolicy();

    private InterpretationEnumerator() {
      interpretations.add(Interpretation.create());
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

    @Override
    public void leavePlainFilter(PlainFilter op) {
      enum0(op.predicate(), plainPredicatePolicy);
    }

    @Override
    public void leaveSubqueryFilter(SubqueryFilter op) {
      enum0(op.predicate(), subqueryPredicatePolicy);
    }

    public static List<Interpretation> enumerate(Graph g) {
      final InterpretationEnumerator enumerator = new InterpretationEnumerator();
      g.acceptVisitor(enumerator);
      return enumerator.interpretations;
    }
  }
}
