package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operator.*;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

import java.util.Deque;
import java.util.LinkedList;

public class MalformedSubqueryFilter extends BaseMatchingRule {
  private final Deque<Integer> stack = new LinkedList<>();

  @Override
  public void leaveInput(Input input) {
    stack.push(1);
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    stack.push(stack.pop() + stack.pop());
  }

  @Override
  public void leaveLeftJoin(LeftJoin op) {
    stack.push(stack.pop() + stack.pop());
  }

  @Override
  public void leaveProj(Proj op) {
    stack.pop();
    stack.push(1);
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    if (stack.pop() != 1) matched = true;
  }

  @Override
  public void leaveUnion(Union op) {
    stack.pop();
  }
}
