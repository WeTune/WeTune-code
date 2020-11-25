package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public abstract class BaseOperator implements Operator {
  private int id;
  private Operator next;
  private final Operator[] prev;
  private final RelationSchema outSchema;

  private Graph graph;
  private Interpreter delegate;

  protected BaseOperator() {
    this(1);
  }

  protected BaseOperator(int numChildren) {
    this.prev = new Operator[numChildren];
    this.outSchema = createOutSchema();
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public Operator next() {
    return next;
  }

  @Override
  public Operator[] prev() {
    return prev;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public RelationSchema outSchema() {
    return outSchema;
  }

  @Override
  public String interpreterName() {
    return (graph == null ? "" : graph.name() == null ? "" : (graph.name() + ".")) + toString();
  }

  @Override
  public Interpretation interpretation() {
    return delegate.interpretation();
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public boolean setNext(Operator next) {
    this.next = next;
    return true;
  }

  @Override
  public boolean setPrev(int idx, Operator prev) {
    if (prev != null) {
      this.prev[idx] = prev;
      prev.setNext(this);
    } else {
      prev = this.prev[idx];
      this.prev[idx] = null;
      if (prev != null) prev.setNext(null);
    }
    return true;
  }

  @Override
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  @Override
  public void setInterpreter(Interpreter interpreter) {
    this.delegate = interpreter;
  }

  @Override
  public void setInterpretation(Interpretation interpretation) {
    delegate.setInterpretation(interpretation);
  }

  @Override
  public Operator copy0() {
    final Operator newInstance = newInstance();
    copyTo(newInstance);
    return newInstance;
  }

  protected abstract Operator newInstance();

  protected void copyTo(Operator other) {}

  protected RelationSchema createOutSchema() {
    return RelationSchema.create(this);
  }
}
