package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.common.utils.SetSupport;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanKind;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.util.Fingerprint;

import java.util.*;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.SetSupport.map;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.stringifyTree;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.LOG;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.normalizePlan;

class TopDownOptimizer implements Optimizer {
  private final SubstitutionBank rules;

  private Memo memo;

  private long startAt;
  private long timeout;

  private boolean tracing, verbose;
  private final Lazy<Map<String, OptimizationStep>> traces;

  TopDownOptimizer(SubstitutionBank rules) {
    this.rules = rules;
    this.traces = Lazy.mk(HashMap::new);
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
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public List<OptimizationStep> traceOf(PlanContext plan) {
    return collectTrace(plan);
  }

  @Override
  public Set<PlanContext> optimize(PlanContext plan) {
    plan = plan.copy();
    int planRoot = plan.root();
    planRoot = enforceInnerJoin(plan, planRoot);
    planRoot = reduceSort(plan, planRoot);

    memo = new Memo();
    startAt = System.currentTimeMillis();

    final Set<SubPlan> subPlans = optimize0(new SubPlan(plan, planRoot));
    return SetSupport.map(subPlans, SubPlan::plan);
  }

  private Set<SubPlan> optimize0(SubPlan subPlan) {
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
    if (isFullyOptimized(subPlan)) return memo.eqClassOf(subPlan);
    else return dispatch(subPlan);
  }

  private Set<SubPlan> optimizeChild(SubPlan n) {
    final Set<SubPlan> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final Set<SubPlan> group = memo.mkEqClass(n);
    group.addAll(candidates);

    return group;
  }

  private Set<SubPlan> optimizeChild0(SubPlan n) {
    final PlanKind kind = n.rootKind();
    assert kind != PlanKind.Input;
    final int numChildren = kind.numChildren();

    // 1. Recursively optimize the children (or retrieve from memo)
    Set<SubPlan> lhsOpts = emptySet(), rhsOpts = emptySet();
    if (numChildren >= 1) lhsOpts = optimize0(n.child(0));
    if (numChildren >= 2) rhsOpts = optimize0(n.child(1));

    Set<SubPlan> opts = emptySet();
    if (numChildren >= 1) {
      opts = map(lhsOpts, lhs -> replaceChild(n, 0, lhs));
    }
    if (numChildren >= 2) {
      opts = map(cartesianProduct(opts, rhsOpts), p -> replaceChild(p.get(0), 1, p.get(1)));
    }

    return opts;
  }

  private Set<SubPlan> optimizeFull(SubPlan n) {
    final Set<SubPlan> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final Set<SubPlan> group = memo.mkEqClass(n);
    group.addAll(candidates);
    // Note: `group` may contain plans that has been fully-optimized.
    // To get rid of duplicated calculation, don't pass the whole `group` to `transform`,
    // pass the `candidates` instead.

    // 4. do transformation
    final List<SubPlan> transformed = ListSupport.flatMap(candidates, this::transform);

    // 5. recursively optimize the transformed plan
    for (SubPlan p : transformed) optimize0(p);

    return group;
  }

  /* find eligible substitutions and use them to transform `n` and generate new plans */
  private Set<SubPlan> transform(SubPlan subPlan) {
    if (System.currentTimeMillis() - startAt >= timeout) return emptySet();
    final PlanContext plan = subPlan.plan();
    final PlanKind kind = subPlan.rootKind();
    final int root = subPlan.nodeId();
    if (kind.isFilter() && plan.kindOf(plan.parentOf(root)).isFilter()) return emptySet();

    final Set<SubPlan> group = memo.eqClassOf(subPlan);
    final Set<SubPlan> transformed = new MinCostSet();
    // 1. fast search for candidate substitution by fingerprint
    final Iterable<Substitution> rules = fastMatchRules(subPlan);
    for (Substitution rule : rules) {
      // 2. full match
      final Match baseMatch = new Match(rule).setSourcePlan(plan).setMatchRootNode(root);
      final List<Match> fullMatches = Match.match(baseMatch, rule._0().root(), root);

      for (Match match : fullMatches) {
        if (match.assembleModifiedPlan()) {
          // 3. generate new plan according to match
          final PlanContext newPlan = match.modifiedPlan();
          int newSubPlanRoot = match.modifiedRootNode();

          final int normalizedRoot = normalizePlan(newPlan, newSubPlanRoot);
          if (normalizedRoot == NO_SUCH_NODE) continue;

          final SubPlan newSubPlan = new SubPlan(newPlan, normalizedRoot);
          // If the `newNode` has been bound with a group, then no need to further optimize it.
          // (because it must either have been or is being optimized.)
          if (!memo.isRegistered(newSubPlan) && group.add(newSubPlan)) {
            transformed.add(newSubPlan);
            traceStep(subPlan.plan(), newSubPlan.plan(), rule);
          }

        } else if (verbose) {
          LOG.log(
              WARNING,
              "instantiation failed: {0}\n{1}\n{2}",
              subPlan,
              rule,
              OptimizerSupport.getLastError());
        }
      }
    }

    transformed.addAll(ListSupport.flatMap(transformed, this::transform));
    return transformed;
  }

  protected Set<SubPlan> onInput(SubPlan input) {
    return memo.mkEqClass(input);
  }

  protected Set<SubPlan> onFilter(SubPlan filter) {
    // only do full optimization at filter chain head
    final PlanContext plan = filter.plan();
    if (plan.kindOf(plan.parentOf(filter.nodeId())).isFilter()) return optimizeChild0(filter);
    else return optimizeFull(filter);
  }

  protected Set<SubPlan> onJoin(SubPlan join) {
    // only do full optimization at join tree root
    final PlanContext plan = join.plan();
    if (plan.kindOf(plan.parentOf(join.nodeId())) == PlanKind.Join) return optimizeChild0(join);
    else return optimizeFull(join);
  }

  protected Set<SubPlan> onProj(SubPlan proj) {
    return optimizeFull(proj);
  }

  protected Set<SubPlan> onLimit(SubPlan limit) {
    return optimizeChild(limit);
  }

  protected Set<SubPlan> onSort(SubPlan sort) {
    return optimizeChild(sort);
  }

  protected Set<SubPlan> onAgg(SubPlan agg) {
    return optimizeChild(agg);
  }

  protected Set<SubPlan> onSetOp(SubPlan setOp) {
    return optimizeChild(setOp);
  }

  private Set<SubPlan> dispatch(SubPlan node) {
    switch (node.rootKind()) {
      case Input:
        return onInput(node);
      case Filter:
      case InSub:
        return onFilter(node);
      case Join:
        return onJoin(node);
      case Proj:
        return onProj(node);
      case Limit:
        return onLimit(node);
      case Sort:
        return onSort(node);
      case Agg:
        return onAgg(node);
      case SetOp:
        return onSetOp(node);
      default:
        throw new IllegalArgumentException();
    }
  }

  private boolean isFullyOptimized(SubPlan subPlan) {
    if (!memo.isRegistered(subPlan)) return false;
    final PlanContext plan = subPlan.plan();
    final int node = subPlan.nodeId();
    final PlanKind kind = subPlan.rootKind();
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      if (!memo.isRegistered(plan, plan.childOf(node, i))) return false;
    }
    return true;
  }

  private Iterable<Substitution> fastMatchRules(SubPlan subPlan) {
    final Set<Fingerprint> fingerprints = Fingerprint.mk(subPlan.plan(), subPlan.nodeId());
    return ListSupport.flatMap(fingerprints, rules::ruleOfFingerprint);
  }

  private List<OptimizationStep> collectTrace(PlanContext plan) {
    return collectTrace0(plan, 0);
  }

  private List<OptimizationStep> collectTrace0(PlanContext key, int depth) {
    if (!traces.isInitialized()) return emptyList();

    final OptimizationStep step = traces.get().get(stringifyTree(key, key.root(), true));
    if (step == null) return new ArrayList<>(depth);
    final List<OptimizationStep> trace = collectTrace0(step.source(), depth + 1);
    trace.add(step);
    return trace;
  }

  private SubPlan replaceChild(SubPlan replaced, int childIdx, SubPlan replacement) {
    final PlanContext replacedPlan = replaced.plan().copy(), replacementPlan = replacement.plan();
    final int replacedSubPlan = replacedPlan.childOf(replaced.nodeId(), childIdx);
    final int replacementSubPlan = replacement.nodeId();
    final ReplaceSubPlan replace = new ReplaceSubPlan(replacedPlan, replacementPlan);
    final int result = replace.replace(replacedSubPlan, replacementSubPlan);
    if (!replacedPlan.isPresent(result)) return null;
    return new SubPlan(replacedPlan, replacedPlan.parentOf(result));
  }

  private int enforceInnerJoin(PlanContext plan, int planRoot) {
    final PlanContext original = tracing ? plan.copy() : null;
    final InnerJoinInference inference = new InnerJoinInference(plan);
    inference.inferenceAndEnforce(planRoot);
    if (inference.isModified() && tracing) traceStep(original, plan, 1);
    return planRoot;
  }

  private int reduceSort(PlanContext plan, int planRoot) {
    final PlanContext original = tracing ? plan.copy() : null;
    final ReduceSort reduceSort = new ReduceSort(plan);
    planRoot = reduceSort.reduce(planRoot);
    if (reduceSort.isReduced() && tracing) traceStep(original, plan, 2);
    return planRoot;
  }

  private void traceStep(PlanContext source, PlanContext target, Substitution rule) {
    if (!tracing) return;
    final String key = stringifyTree(target, target.root(), true);
    traces.get().computeIfAbsent(key, ignored -> new OptimizationStep(source, target, rule, 0));
  }

  private void traceStep(PlanContext source, PlanContext target, int extra) {
    if (!tracing) return;
    final String key = stringifyTree(target, target.root(), true);
    traces.get().computeIfAbsent(key, ignored -> new OptimizationStep(source, target, null, extra));
  }
}
