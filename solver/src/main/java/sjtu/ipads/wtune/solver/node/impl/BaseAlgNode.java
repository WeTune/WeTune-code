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

  private final boolean isForcedDistinct;

  private final List<AlgNode> inputs;
  private List<ColumnRef> inputCols;
  private List<SymbolicColumnRef> inputSymCols;

  protected SolverContext ctx;

  protected BaseAlgNode() {
    this(false, new ArrayList<>());
  }

  protected BaseAlgNode(boolean isForcedDistinct, List<AlgNode> inputs) {
    this.isForcedDistinct = isForcedDistinct;
    this.inputs = inputs;
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public List<AlgNode> inputs() {
    return inputs;
  }

  @Override
  public boolean isForcedUnique() {
    return isForcedDistinct;
  }

  @Override
  public AlgNode setNamespace(String namespace) {
    this.namespace = namespace;
    inputsAndSubquery().forEach(it -> it.setNamespace(namespace));
    return this;
  }

  @Override
  public AlgNode setSolverContext(SolverContext context) {
    ctx = context;
    inputsAndSubquery().forEach(it -> it.setSolverContext(context));
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
