package sjtu.ipads.wtune.solver.node.impl;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseAlgNode implements AlgNode {
  private String namespace;

  private AlgNode parent;

  private final List<AlgNode> inputs;
  protected SolverContext ctx;

  private List<ColumnRef> inputCols;
  private List<SymbolicColumnRef> inputSymCols;

  protected BaseAlgNode() {
    this(new ArrayList<>());
  }

  protected BaseAlgNode(List<AlgNode> inputs) {
    this.inputs = inputs;
    this.inputs.forEach(it -> it.setParent(this));
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public AlgNode parent() {
    return parent;
  }

  @Override
  public List<AlgNode> inputs() {
    return inputs;
  }

  @Override
  public AlgNode setNamespace(String namespace) {
    this.namespace = namespace;
    inputs().forEach(it -> it.setNamespace(namespace));
    return this;
  }

  @Override
  public AlgNode setParent(AlgNode parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public AlgNode setSolverContext(SolverContext context) {
    ctx = context;
    inputs().forEach(it -> it.setSolverContext(context));
    return this;
  }

  protected List<ColumnRef> inputColumns() {
    if (inputCols == null)
      inputCols =
          inputs.stream()
              .map(AlgNode::columns)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());

    return inputCols;
  }

  protected List<SymbolicColumnRef> inputSymbolicColumns() {
    if (inputSymCols == null)
      inputSymCols =
          inputs.stream()
              .map(AlgNode::projected)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());

    return inputSymCols;
  }
}
