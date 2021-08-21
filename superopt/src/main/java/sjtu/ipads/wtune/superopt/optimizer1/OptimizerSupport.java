package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;

public interface OptimizerSupport {
  static JoinNode normalizeJoinTree(JoinNode joinRoot) {
    return JoinTreeNormalizer.normalize(joinRoot);
  }

  static LinearJoinTree linearizeJoinTree(JoinNode joinRoot) {
    return LinearJoinTreeImpl.mk(joinRoot);
  }
}
