package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.superopt.fragment.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.util.*;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.replaceGlobal;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PROJ;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.normalizePlan;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.reduceSort;
import static sjtu.ipads.wtune.superopt.optimizer.ReversedMatch.reversedMatch;

class OptimizerImpl implements Optimizer {
  private final SubstitutionBank bank;

  private Memo memo;

  private boolean tracing;
  private Map<String, OptimizationStep> traces;

  private long startAt;
  private long timeout;

  OptimizerImpl(SubstitutionBank bank) {
    this.bank = bank;
    this.startAt = Long.MIN_VALUE;
    this.timeout = Long.MAX_VALUE;
  }

  @Override
  public void setTracing(boolean flag) {
    tracing = flag;
  }

  @Override
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public List<OptimizationStep> traceOf(PlanNode plan) {
    return collectTrace(plan);
  }

  @Override
  public Set<PlanNode> optimize(PlanNode plan) {
    final PlanNode plan0 = normalizePlan(plan);
    final PlanNode plan1 = reduceSort(plan0);
    final boolean sortReduced = plan0 != plan1;

    memo = new Memo();
    startAt = System.currentTimeMillis();
    if (tracing) {
      traces = new HashMap<>();
      if (sortReduced) traceStep(plan0, plan1, null /* stand for reduce-sort */);
    }

    final Set<PlanNode> ret = optimize0(plan1);
    return ret;
  }

  private Set<PlanNode> optimize0(PlanNode node) {
    // A plan is "fully optimized" if:
    // 1. itself has been registered in memo, and
    // 2. all its children have been registered in memo.
    // Such a plan won't be transformed again.
    //
    // Note that the other plans in the same group of a fully-optimized
    // plan still have chance to be transformed.
    //
    // Example:
    // P0(cost=2) -> P1(cost=2),P2(cost=2); P2 -> P3(cost=1)
    // Now, P1 cannot be further transformed (nor its children), thus "fully-optimized"
    // Obviously, P1 and P3 is equivalent. P1 and P3 thus reside in the same group.
    // Then, when P2 are transformed to P3, the group is accordingly updated.
    if (memo.isRegistered(node) && stream(node.predecessors()).allMatch(memo::isRegistered))
      return memo.eqClassOf(node);
    else return dispatch(node);
  }

  private Set<PlanNode> optimizeChild0(PlanNode n) {
    // 1. Recursively optimize the children (or retrieve from memo)
    final Set<List<PlanNode>> childOpts;
    if (n.kind().numPredecessors() == 1)
      childOpts = cartesianProduct(optimize0(n.predecessors()[0]));
    else
      childOpts = cartesianProduct(optimize0(n.predecessors()[0]), optimize0(n.predecessors()[1]));

    // 2. Attach optimized children with this node
    final Set<PlanNode> candidates = applyChildrenOpts(n, childOpts);
    assert !candidates.isEmpty();

    return candidates;
  }

  private Set<PlanNode> optimizeChild(PlanNode n) {
    assert n.kind() != PROJ && n.kind() != INPUT;
    final Set<PlanNode> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final Set<PlanNode> group = memo.mkEqClass(n);
    group.addAll(candidates);

    return group;
  }

  private Set<PlanNode> optimizeFull(PlanNode n) {
    final Set<PlanNode> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final Set<PlanNode> group = memo.mkEqClass(n);
    group.addAll(candidates);
    // Note: `group` may contain plans that has been fully-optimized.
    // To get rid of duplicated calculation, don't pass the whole `group` to `transform`,
    // pass the `candidates` instead.

    // 4. do transformation
    final List<PlanNode> transformed = listFlatMap(candidates, this::transform);

    // 5. recursively optimize the transformed plan
    for (PlanNode p : transformed) optimize0(p);

    return group;
  }

  /* find eligible substitutions and use them to transform `n` and generate new plans */
  private Set<PlanNode> transform(PlanNode n) {
    if (System.currentTimeMillis() - startAt >= timeout) return emptySet();
    if (n.kind().isFilter() && n.successor().kind().isFilter()) return emptySet();

    final Set<PlanNode> group = memo.eqClassOf(n);
    final Set<PlanNode> transformed = new MinCostSet<>(memo::extractKey);
    // 1. fast search for candidate substitution by fingerprint
    final Iterable<Substitution> substitutions = bank.matchByFingerprint(n);
    for (Substitution substitution : substitutions) {
      final ConstraintAwareModel model =
          ConstraintAwareModel.mk(n.context(), substitution.constraints());
      // 2. full match
      final List<Match> matches = match(n, substitution._0().root(), model);

      for (Match match : matches) {
        // 3. generate new plan according to match
        if (!preValidate(match.model())) continue;
        final PlanNode substituted = match.substitute(substitution._1());
        if (!postValidate(substituted)) continue;
        final PlanNode newNode = normalizePlan(substituted);

        // If the `newNode` has been bound with a group, then no need to further optimize it.
        // (because it must either have been or is being optimized.)
        if (!memo.isRegistered(newNode) && group.add(newNode)) {
          transformed.add(newNode);
          traceStep(n, newNode, substitution);
        }
      }
    }

    transformed.addAll(listFlatMap(transformed, this::transform));
    return transformed;
  }

  private List<Match> match(PlanNode node, Op op, ConstraintAwareModel baseModel) {
    final List<Match> ret = new ArrayList<>();

    for (PlanNode n : reversedMatch(node, op, baseModel)) {
      final ConstraintAwareModel model = baseModel.derive(n.context());

      if (!op.match(n, model) || !model.checkConstraint(false)) continue;

      final PlanNode[] nodePredecessors = n.predecessors();
      final Op[] opPredecessors = op.predecessors();

      // Note:
      // 1. Each child may have multiple matches
      // 2. Following matches can be affected by previous matches.
      // Thus, `matches` remembers the match results so far, and is passed to next match attempt.
      List<Match> matches = singletonList(new Match(n, model));
      for (int i = 0, bound = opPredecessors.length; i < bound; i++) {
        final PlanNode nextNode = nodePredecessors[i];
        final Op nextOp = opPredecessors[i];

        matches = listFlatMap(matches, it -> match(nextNode, nextOp, it.model()));
        if (matches.isEmpty()) break;

        matches.forEach(Match::shiftMatchPoint); // Shift the match point back to `n`.
      }

      ret.addAll(matches);
    }

    return ret;
  }

  /**
   * apply `childrenOpts` to `root` to build new plans.
   *
   * @param childrenOpts: the 1st dim is possibilities, the 2nd dim is the optimized children
   * @return fresh new plan constructed
   */
  private static Set<PlanNode> applyChildrenOpts(
      PlanNode parent, Set<List<PlanNode>> childrenOpts) {
    final Set<PlanNode> ret = new HashSet<>(childrenOpts.size());

    for (List<PlanNode> optChildren : childrenOpts) {
      if (optChildren.isEmpty()) continue;

      assert optChildren.size() == parent.kind().numPredecessors();
      final PlanNode newNode = replaceGlobal(parent, optChildren);
      if (newNode != parent) {
        zipForEach(
            asList(parent.predecessors()),
            asList(newNode.predecessors()),
            OptimizerSupport::alignOutValues);
        if (!newNode.rebindRefs(parent.context())) {
          // For Sort node the rebinding may fail.
          continue;
        }
        newNode.context().clearRedirections();
      }
      ret.add(newNode);
    }

    if (ret.isEmpty()) ret.add(parent);
    return ret;
  }

  protected Set<PlanNode> onInput(InputNode input) {
    return singleton(input);
  }

  protected Set<PlanNode> onFilter(FilterNode filter) {
    assert filter.successor() != null;
    // only do full optimization at filter chain head
    if (filter.successor().kind().isFilter()) return optimizeChild0(filter);
    else return optimizeFull(filter);
  }

  protected Set<PlanNode> onJoin(JoinNode join) {
    // only do full optimization at join tree root
    if (join.successor().kind().isJoin()) return optimizeChild0(join);
    else return optimizeFull(join);
  }

  protected Set<PlanNode> onProj(ProjNode proj) {
    return optimizeFull(proj);
  }

  protected Set<PlanNode> onLimit(LimitNode limit) {
    return optimizeChild(limit);
  }

  protected Set<PlanNode> onSort(SortNode sort) {
    return optimizeChild(sort);
  }

  protected Set<PlanNode> onAgg(AggNode agg) {
    return optimizeChild(agg);
  }

  private Set<PlanNode> dispatch(PlanNode node) {
    switch (node.kind()) {
      case INPUT:
        return onInput((InputNode) node);
      case SIMPLE_FILTER:
      case IN_SUB_FILTER:
        return onFilter((FilterNode) node);
      case INNER_JOIN:
      case LEFT_JOIN:
        return onJoin((JoinNode) node);
      case PROJ:
        return onProj((ProjNode) node);
      case LIMIT:
        return onLimit((LimitNode) node);
      case SORT:
        return onSort((SortNode) node);
      case AGG:
        return onAgg((AggNode) node);
      default:
        throw new IllegalArgumentException();
    }
  }

  private boolean preValidate(ConstraintAwareModel model) {
    return model.checkConstraint(true);
  }

  private boolean postValidate(PlanNode substituted) {
    final OperatorType kind = substituted.kind();
    return substituted.successor() != null || (!kind.isJoin() && !kind.isFilter() && kind != INPUT);
  }

  private List<OptimizationStep> collectTrace(PlanNode node) {
    return collectTrace0(treeRootOf(node).toString(true), 0);
  }

  private List<OptimizationStep> collectTrace0(String key, int depth) {
    final OptimizationStep step = traces.get(key);
    if (step == null) return new ArrayList<>(depth);
    final List<OptimizationStep> trace = collectTrace0(step.original(), depth + 1);
    trace.add(step);
    return trace;
  }

  private void traceStep(PlanNode original, PlanNode transformed, Substitution substitution) {
    if (!tracing) return;
    final String originalKey = treeRootOf(original).toString(true);
    final String newKey = treeRootOf(transformed).toString(true);
    traces.computeIfAbsent(newKey, ignored -> new OptimizationStep(originalKey, substitution));
  }
}
