package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;

public class InnerJoinNodeImpl extends JoinNodeBase implements InnerJoinNode {
  protected InnerJoinNodeImpl(ASTNode onCondition) {
    super(onCondition);
  }

  public static InnerJoinNode build(ASTNode onCondition) {
    return new InnerJoinNodeImpl(onCondition);
  }
}
