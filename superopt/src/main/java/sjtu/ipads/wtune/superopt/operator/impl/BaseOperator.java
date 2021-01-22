package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.Operator;

public abstract class BaseOperator implements Operator {
  private Graph graph;
  private final Operator[] predecessors;

  protected BaseOperator() {
    predecessors = new Operator[type().numPredecessors()];
  }

  @Override
  public Operator[] predecessors() {
    return predecessors;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public void setPredecessor(int idx, Operator prev) {
    this.predecessors[idx] = prev;
  }

  @Override
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  @Override
  public String toString() {
    return type().name();
  }

  protected abstract Operator newInstance();

  protected Placeholder newPlaceholder(String tag) {
    return new PlaceholderImpl(this, tag);
  }

  private static class PlaceholderImpl implements Placeholder {
    private final Operator scope;
    private final String tag;
    private int index;

    private PlaceholderImpl(Operator scope, String tag, int index) {
      this.scope = scope;
      this.tag = tag;
      this.index = index;
    }

    private PlaceholderImpl(Operator scope, String tag) {
      this(scope, tag, 0);
    }

    @Override
    public Object scope() {
      return scope.graph();
    }

    @Override
    public String tag() {
      return tag;
    }

    @Override
    public int index() {
      return index;
    }

    @Override
    public void setIndex(int index) {
      this.index = index;
    }

    @Override
    public Placeholder copy() {
      return new PlaceholderImpl(scope, tag, index);
    }

    @Override
    public String toString() {
      return tag() + index();
    }
  }
}
