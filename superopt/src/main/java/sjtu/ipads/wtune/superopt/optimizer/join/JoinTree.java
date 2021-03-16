package sjtu.ipads.wtune.superopt.optimizer.join;

import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

public interface JoinTree extends List<JoinNode> {
  PlanNode originalRoot();

  JoinNode rebuild();

  JoinNode copyAndRebuild();

  JoinTree withRoot(int rootIdx);

  JoinTree sorted();

  boolean isValidRoot(int rootIdx);

  static PlanNode predecessorOfTree(JoinNode root) {
    PlanNode predecessor = root;
    while (predecessor.type().isJoin()) predecessor = predecessor.predecessors()[0];
    return predecessor;
  }

  static JoinTree make(JoinNode root) {
    return JoinTreeImpl.make(root);
  }
}
