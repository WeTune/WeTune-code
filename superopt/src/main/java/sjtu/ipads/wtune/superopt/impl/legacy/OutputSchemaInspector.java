package sjtu.ipads.wtune.superopt.impl.legacy;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.operators.*;

import java.util.Stack;

public class OutputSchemaInspector implements GraphVisitor {
  private final Stack<OutputSchema> stack = new Stack<>();

  @Override
  public void leaveInput(Input input) {
    stack.push(OutputSchema.fromOp(input));
  }

  @Override
  public void leaveJoin(Join op) {
    stack.push(OutputSchema.fromJoin(stack.pop(), stack.pop()));
  }

  @Override
  public void leaveAgg(Agg op) {
    stack.push(OutputSchema.fromAgg(op, stack.pop()));
  }

  @Override
  public void leaveProj(Proj op) {
    stack.push(OutputSchema.fromProj(stack.pop()));
  }

  @Override public void leaveSubqueryFilter(SubqueryFilter op) {
    stack.pop();
  }

  public static OutputSchema inspect(Graph graph) {
    final OutputSchemaInspector visitor = new OutputSchemaInspector();
    graph.acceptVisitor(visitor);
    return visitor.stack.pop();
  }
}
