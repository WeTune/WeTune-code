package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

import java.util.Deque;
import java.util.LinkedList;

public class ToASTTranslator implements PlanVisitor {
  private final Deque<ASTNode> stack;

  private ToASTTranslator() {
    stack = new LinkedList<>();
  }

  public static ASTNode translate(Plan plan) {
    return null;
  }
}
