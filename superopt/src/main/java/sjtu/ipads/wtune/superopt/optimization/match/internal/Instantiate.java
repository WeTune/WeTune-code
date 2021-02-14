package sjtu.ipads.wtune.superopt.optimization.match.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.superopt.optimization.Operator;
import sjtu.ipads.wtune.superopt.optimization.match.Interpretations;
import sjtu.ipads.wtune.superopt.plan.InnerJoin;
import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;

public class Instantiate implements PlanVisitor {
  private Interpretations interpretations;
  private final Deque<Operator> stack = new LinkedList<>();

  @Override
  public void leaveInput(Input input) {
    stack.add(interpretations.interpretInput(input.table()).operator());
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    final List<Attribute> leftKeys = interpretations.interpretPick(op.leftFields()).projection();
    final List<Attribute> rightKeys = interpretations.interpretPick(op.rightFields()).projection();

    assert leftKeys.size() == rightKeys.size();

    final List<ASTNode> onConditions = new ArrayList<>(leftKeys.size());
    for (int i = 0, bound = leftKeys.size(); i < bound; i++) {
      final Attribute leftKey = leftKeys.get(i), rightKey = rightKeys.get(i);
      final ASTNode eqExpr = ASTNode.expr(ExprKind.BINARY);
      eqExpr.set(BINARY_OP, BinaryOp.EQUAL);
      eqExpr.set(BINARY_LEFT, leftKey.toColumnRef());
      eqExpr.set(BINARY_RIGHT, rightKey.toColumnRef());
      onConditions.add(eqExpr);
    }
  }
}
