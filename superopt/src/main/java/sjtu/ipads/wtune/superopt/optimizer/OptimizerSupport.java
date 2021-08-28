package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.setMap;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.assemblePlan;

public interface OptimizerSupport {
  static Set<ASTNode> optimize(SubstitutionBank bank, Schema schema, ASTNode ast) {
    final Set<PlanNode> optimized = Optimizer.mk(bank).optimize(assemblePlan(ast, schema));
    return setMap(optimized, PlanSupport::translateAsAst);
  }

  static Set<PlanNode> optimize(SubstitutionBank bank, PlanNode node) {
    return Optimizer.mk(bank).optimize(node);
  }

  static PlanNode reduceSort(PlanNode node) {
    return new SortReducer().reduceSort(node);
  }

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
