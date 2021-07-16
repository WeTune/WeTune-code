package sjtu.ipads.wtune.superopt.optimizer.internal;

import static com.google.common.collect.Lists.cartesianProduct;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;
import static sjtu.ipads.wtune.sqlparser.ASTContext.manage;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Input;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.copyOnTree;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.copyToRoot;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedOnTree;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.rootOf;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.toStringOnTree;
import static sjtu.ipads.wtune.sqlparser.plan.ToASTTranslator.toAST;
import static sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator.toPlan;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.findInvalidColumnRefs;
import static sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations.constrainedInterpretations;
import static sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations.derivedInterpretations;
import static sjtu.ipads.wtune.superopt.optimizer.support.DistinctReducer.reduceDistinct;
import static sjtu.ipads.wtune.superopt.optimizer.support.PlanNormalizer.normalize;
import static sjtu.ipads.wtune.superopt.optimizer.support.SortReducer.reduceSort;
import static sjtu.ipads.wtune.superopt.optimizer.support.UniquenessInference.inferUniqueness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan.SortNode;
import sjtu.ipads.wtune.sqlparser.plan.TypeBasedAlgorithm;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimizer.Hint;
import sjtu.ipads.wtune.superopt.optimizer.Match;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerException;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;
import sjtu.ipads.wtune.superopt.optimizer.support.Memo;
import sjtu.ipads.wtune.superopt.optimizer.support.MinCostList;
import sjtu.ipads.wtune.superopt.optimizer.support.OptGroup;

public class OptimizerImpl extends TypeBasedAlgorithm<List<PlanNode>> implements Optimizer {
  private static final long TIMEOUT = 20_000; // 20 second

  private final SubstitutionBank repo;
  private final Schema schema;
  private final Memo<String> memo;

  private long startTime = 0;

  private boolean tracing;
  private Map<String, Step> traces;
  private List<List<Substitution>> optimizationTraces;

  public OptimizerImpl(SubstitutionBank repo, Schema schema) {
    this.repo = repo;
    this.schema = schema;
    this.memo = new Memo<>(PlanNode::toStringOnTree);
  }

  @Override
  public void setTracing(boolean logging) {
    this.tracing = logging;
    if (this.traces == null) this.traces = new HashMap<>();
  }

  @Override
  public List<List<Substitution>> getTraces() {
    return optimizationTraces;
  }

  @Override
  public List<ASTNode> optimize(ASTNode root) {
    root = root.deepCopy();
    root.context().setSchema(schema);
    root.context().setDbType(null); // temporary fix

    final List<PlanNode> optimizedPlans = optimize(toPlan(root));
    final List<ASTNode> nodes = new ArrayList<>(optimizedPlans.size());
    for (PlanNode optimizedPlan : optimizedPlans) {
      final ASTNode ast = manage(toAST(optimizedPlan), schema);
      if (findInvalidColumnRefs(ast) == null) nodes.add(ast);
    }

    return nodes;
  }

  @Override
  public List<PlanNode> optimize(PlanNode root) {
    if (root == null) return emptyList();

    memo.clear();
    if (tracing) traces.clear();

    try {
      // preprocess
      final boolean reduced = reduceSort(root) || reduceDistinct(root);
      final PlanNode normalized = normalize(root);
      assert normalized != null;

      startTime = System.currentTimeMillis(); // begin timing

      List<PlanNode> optimized = optimize0(normalized);
      assert !optimized.isEmpty();

      // exclude the original query (if it is included, it must be the head of the list)
      if (!reduced && toStringOnTree(optimized.get(0)).equals(toStringOnTree(normalized)))
        optimized = optimized.subList(1, optimized.size());

      if (tracing) optimizationTraces = listMap(optimized, this::collectTrace);

      return optimized;
    } catch (OptimizerException ex) {
      // PlanException indicates the are something unsupported,
      // so no need to throw out
      return emptyList();
    }
  }

  @Override
  protected List<PlanNode> onInput(InputNode input) {
    return singletonList(input);
  }

  @Override
  protected List<PlanNode> onPlainFilter(FilterNode filter) {
    assert filter.successor() != null;
    // only do full optimization at filter chain head
    if (filter.successor().type().isFilter()) return optimizeChild0(filter);
    else return optimizeFull(filter);
  }

  @Override
  protected List<PlanNode> onSubqueryFilter(FilterNode filter) {
    assert filter.successor() != null;
    // only do full optimization at filter chain head
    if (filter.successor().type().isFilter()) return optimizeChild0(filter);
    else return optimizeFull(filter);
  }

  @Override
  protected OptGroup<?> onProj(ProjNode proj) {
    return optimizeFull(proj);
  }

  @Override
  protected List<PlanNode> onInnerJoin(JoinNode innerJoin) {
    // only do full optimization at join tree root
    if (innerJoin.successor().type().isJoin()) return optimizeChild0(innerJoin);
    else return optimizeFull(innerJoin);
  }

  @Override
  protected List<PlanNode> onLeftJoin(JoinNode leftJoin) {
    // only do full optimization at join tree root
    if (leftJoin.successor().type().isJoin()) return optimizeChild0(leftJoin);
    else return optimizeFull(leftJoin);
  }

  @Override
  protected OptGroup<?> onLimit(LimitNode limit) {
    return optimizeChild(limit);
  }

  @Override
  protected OptGroup<?> onSort(SortNode sort) {
    return optimizeChild(sort);
  }

  @Override
  protected OptGroup<?> onAgg(AggNode agg) {
    return optimizeChild(agg);
  }

  private List<PlanNode> optimize0(PlanNode node) {
    final String key = toStringOnTree(node);
    final OptGroup<?> group = memo.get(key);
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
    // Obviously, P1 and P3 is equivalent. P1 and P2 thus reside in the same OptGroup.
    // Then, when P2 are transformed to P3, the group is accordingly updated.
    if (group != null && stream(node.predecessors()).allMatch(it -> memo.get(it) != null))
      return group;
    else return dispatch(node);
  }

  private List<PlanNode> optimizeChild0(PlanNode n) {
    // 1. Recursively optimize the children (or retrieve from memo)
    final List<List<PlanNode>> childOpts;
    if (n.type().numPredecessors() == 1)
      childOpts = cartesianProduct(optimize0(n.predecessors()[0]));
    else // assert n.type.numPredecessors() == 2
    childOpts = cartesianProduct(optimize0(n.predecessors()[0]), optimize0(n.predecessors()[1]));

    // 2. Attach optimized children with this node
    final List<PlanNode> candidates = applyChildrenOpts(n, childOpts);
    assert !candidates.isEmpty();

    return candidates;
  }

  private OptGroup<?> optimizeChild(PlanNode n) {
    assert n.type() != Proj && n.type() != Input;
    final List<PlanNode> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final OptGroup<String> group = memo.makeGroup(n);
    group.addAll(candidates);

    return group;
  }

  private OptGroup<?> optimizeFull(PlanNode n) {
    final List<PlanNode> candidates = optimizeChild0(n);

    // 3. Register the candidates. See `optimized0` for explanation.
    final OptGroup<String> group = memo.makeGroup(n);
    group.addAll(candidates);
    // Note: `group` may contain plans that has been fully-optimized.
    // To get rid of duplicated calculation, don't pass the whole `group` to `transform`,
    // pass the `candidates` instead.

    // 4. do transformation
    final List<PlanNode> transformed = listFlatMap(this::transform, candidates);

    // 5. recursively optimize the transformed plan
    for (PlanNode p : transformed) optimize0(p);

    return group;
  }

  /* find eligible substitutions and use them to transform `n` and generate new plans */
  private List<PlanNode> transform(PlanNode n) {
    if (System.currentTimeMillis() - startTime >= TIMEOUT) return emptyList();
    if (n.type().isFilter() && n.successor().type().isFilter()) return emptyList();
    if (!inferUniqueness(n)) return emptyList();

    final OptGroup<String> group = memo.get(n);

    final List<PlanNode> transformed = new MinCostList();
    // 1. fast search for candidate substitution by fingerprint
    final List<Substitution> substitutions = repo.findByFingerprint(n);
    for (Substitution substitution : substitutions) {
      final Interpretations inter = constrainedInterpretations(substitution.constraints());
      // 2. full match
      final List<Match> matches = match(n, substitution.g0().head(), inter);

      for (Match match : matches) {
        // 3. generate new plan according to match
        final PlanNode newNode = normalize(match.substitute(substitution.g1()));
        if (newNode == null) continue; // invalid, abandon it
        if (!inferUniqueness(newNode)) {
          if (newNode.type() == Proj) ((ProjNode) newNode).setForcedUnique(true);
          else continue; // unable to enforce uniqueness
        }

        // If the `newNode` has been bound with a group, then no need to further optimize it.
        // (because it must either have been or is being optimized.)
        if (memo.get(newNode) == null) {
          transformed.add(newNode);
          traceStep(n, newNode, substitution);
        }
        group.add(newNode);
      }
    }

    transformed.addAll(listFlatMap(this::transform, transformed));
    return transformed;
  }

  private static List<Match> match(PlanNode node, Operator op, Interpretations baseInter) {
    final List<Match> ret = new ArrayList<>();

    for (PlanNode n : Hint.apply(node, op, baseInter)) {
      // setup a new Interpretations to isolate each possibility
      final Interpretations inter = derivedInterpretations(baseInter);

      if (!op.match(n, inter)) continue;

      final PlanNode[] nodePredecessors = n.predecessors();
      final Operator[] opPredecessors = op.predecessors();

      // Note:
      // 1. Each child may have multiple matches
      // 2. Following matches can be affected by previous matches.
      // Thus, `matches` remembers the match results so far, and is passed to next match attempt.
      List<Match> matches = singletonList(Match.make(n, inter));
      for (int i = 0, bound = opPredecessors.length; i < bound; i++) {
        final PlanNode nextNode = nodePredecessors[i];
        final Operator nextOp = opPredecessors[i];

        matches = listFlatMap(it -> match(nextNode, nextOp, it.assignments()), matches);
        if (matches.isEmpty()) break;
        // lift the match point
        matches = listMap(matches, Match::lift);
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
  private static List<PlanNode> applyChildrenOpts(
      PlanNode root, List<List<PlanNode>> childrenOpts) {
    final List<PlanNode> ret = new ArrayList<>(childrenOpts.size());

    for (List<PlanNode> optChildren : childrenOpts) {
      if (optChildren.isEmpty()) continue;
      assert optChildren.size() == root.type().numPredecessors();

      final PlanNode copy = copyToRoot(root);
      for (int i = 0, bound = optChildren.size(); i < bound; i++) {
        final PlanNode optChild = optChildren.get(i);
        copy.setPredecessor(i, copyOnTree(optChild));
      }

      resolveUsedOnTree(rootOf(copy));
      ret.add(copy);
    }

    return ret;
  }

  private void traceStep(PlanNode original, PlanNode transformed, Substitution substitution) {
    if (!tracing) return;
    final String originalKey = toStringOnTree(rootOf(original));
    final String newKey = toStringOnTree(rootOf(transformed));
    traces.putIfAbsent(newKey, new Step(originalKey, substitution));
  }

  private List<Substitution> collectTrace(PlanNode node) {
    return collectTrace0(toStringOnTree(rootOf(node)), 0);
  }

  private List<Substitution> collectTrace0(String key, int depth) {
    final Step step = traces.get(key);
    if (step == null) return new ArrayList<>(depth);

    final List<Substitution> trace = collectTrace0(step.original, depth + 1);
    trace.add(step.substitution);
    return trace;
  }

  private static class Step {
    private final String original;
    private final Substitution substitution;

    private Step(String original, Substitution substitution) {
      this.original = original;
      this.substitution = substitution;
    }
  }
}
