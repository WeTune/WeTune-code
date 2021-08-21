package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

public interface OptimizerSupport {
  static FilterNode normalizeFilterChain(FilterNode filterRoot) {
    return FilterChainNormalizer.normalize(filterRoot);
  }

  static JoinNode normalizeJoinTree(JoinNode joinRoot) {
    return JoinTreeNormalizer.normalize(joinRoot);
  }

  static LinearJoinTree linearizeJoinTree(JoinNode joinRoot) {
    return LinearJoinTreeImpl.mk(joinRoot);
  }

  static PlanNode insertProjIfNeed(PlanNode node) {
    return ProjNormalizer.insertProjIfNeed(node);
  }

  static PlanNode removeProjIfNeed(PlanNode node) {
    return ProjNormalizer.removeProjIfNeed(node);
  }

  static PlanNode removeDedupIfNeed(PlanNode node) {
    return ProjNormalizer.removeDedupIfNeed(node);
  }

  static PlanNode inferenceInnerJoin(PlanNode node) {
    return EffectiveInnerJoinInference.inference(node);
  }

  static PlanNode normalizePlan(PlanNode root) {
    return PlanNormalizer.normalize(root);
  }
}
