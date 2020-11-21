package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.operators.Proj;

import java.util.Stack;

public class OutputEstimator implements GraphVisitor {
  private final Stack<OutputEstimation> stack = new Stack<>();

  @Override
  public void enterEmpty(Operator parent, int idx) {
    stack.push(OutputEstimation.init());
  }

  @Override
  public void leaveJoin(Join op) {
    stack.push(OutputEstimation.of(op, stack.pop(), stack.pop()));
  }

  @Override
  public void leaveProj(Proj op) {
    stack.push(OutputEstimation.of(op, stack.pop()));
  }

  @Override
  public void leaveAgg(Agg op) {
    stack.push(OutputEstimation.of(op, stack.pop()));
  }

  public static OutputEstimation estimateOutput(Graph graph) {
    final OutputEstimator estimator = new OutputEstimator();
    graph.acceptVisitor(estimator);
    return estimator.stack.pop();
  }
}
