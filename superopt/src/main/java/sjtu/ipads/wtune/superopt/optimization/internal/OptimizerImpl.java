package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.*;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.cartesianProduct;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Input;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.*;
import static sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator.toPlan;
import static sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations.constrainedInterpretations;
import static sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations.derivedInterpretations;
import static sjtu.ipads.wtune.superopt.internal.DistinctReducer.reduceDistinct;
import static sjtu.ipads.wtune.superopt.internal.PlanNormalizer.normalize;
import static sjtu.ipads.wtune.superopt.internal.SortReducer.reduceSort;

public class OptimizerImpl extends TypeBasedAlgorithm<List<PlanNode>> implements Optimizer {
  private final SubstitutionBank repo;
  private final Schema schema;
  private final Memo<String> memo;

  public OptimizerImpl(SubstitutionBank repo, Schema schema) {
    this.repo = repo;
    this.schema = schema;
    this.memo = new Memo<>(this::extractKey);
  }

  @Override
  public List<ASTNode> optimize(ASTNode root) {
    return listMap(this::toAST, optimize(toPlan(root)));
  }

  @Override
  public List<PlanNode> optimize(PlanNode root) {
    memo.clear();

    reduceSort(root);
    reduceDistinct(root);

    final PlanNode normalized = normalize(root);
    assert normalized != null;

    return optimize0(normalized);
  }

  private String extractKey(PlanNode root) {
    if (root.type() != Input) return root.type().toString() + ":" + toAST(root);
    else return root.type().toString() + "@" + ((InputNode) root).id() + ":" + toAST(root);
  }

  private List<PlanNode> optimize0(PlanNode node) {
    final String key = extractKey(node);
    final OptGroup<?> group = memo.get(key);
    // Note: to prevent redundant optimization, we check whether a plan is "optimized"
    // A plan is optimized if:
    // 1. it self has be registered in memo, and
    // 2. all its children has been registered in memo
    //
    // Note that the group of an "optimized" plan still has chance to be updated,
    // since other plan in the same group may be further transformed.
    // Consider this situation:
    // P0(cost=2) -> P1(cost=2),P2(cost=2); P2 -> P3(cost=1)
    // After trying, P1 cannot be further transformed (nor its children), thus "optimized"
    // Obviously, P1 and P3 is equivalent. Thus P1 and P2 are in same OptGroup.
    // Then, when P2 are transformed to P3, the group is accordingly updated.
    //
    // This mechanism can also prevent cyclic transformation, i.e. A -> B -> A
    if (group != null && stream(node.predecessors()).allMatch(it -> memo.get(it) != null))
      return group;

    return dispatch(node);
  }

  @Override
  protected List<PlanNode> onInput(InputNode input) {
    return singletonList(input);
  }

  @Override
  protected List<PlanNode> onPlainFilter(PlainFilterNode filter) {
    assert filter.successor() != null;
    // only do full optimization at filter chain head
    if (filter.successor().type().isFilter()) return optimizeChild0(filter);
    else return optimizeFull(filter);
  }

  @Override
  protected List<PlanNode> onSubqueryFilter(SubqueryFilterNode filter) {
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
  protected List<PlanNode> onInnerJoin(InnerJoinNode innerJoin) {
    // only do full optimization at join tree root
    if (innerJoin.successor().type().isJoin()) return optimizeChild0(innerJoin);
    else return optimizeFull(innerJoin);
  }

  @Override
  protected List<PlanNode> onLeftJoin(LeftJoinNode leftJoin) {
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

  private List<PlanNode> optimizeChild0(PlanNode n) {
    // 1. Recursively optimize the children (or retrieve from memo)
    final List<List<PlanNode>> childOpts;
    if (n.type().numPredecessors() == 1)
      childOpts = cartesianProduct(optimize0(n.predecessors()[0]));
    else
      childOpts = cartesianProduct(optimize0(n.predecessors()[0]), optimize0(n.predecessors()[1]));

    // 2. Combined optimized children with this node
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
    // Note: `group` may contain plans that has been transformed. To get rid of duplicated
    // calculation, don't pass whole `group` to `transform`, pass `candidates` instead.

    // 4. do transformation
    final List<PlanNode> transformed = listFlatMap(this::transform, candidates);

    // 5. recursively optimize the transformed plan
    for (PlanNode p : transformed) optimize0(p);

    return group;
  }

  private ASTNode toAST(PlanNode plan) {
    return ASTContext.manage(schema, ToASTTranslator.toAST(plan));
  }

  /* find eligible substitutions and use them to transform `n` and generate new plans */
  private List<PlanNode> transform(PlanNode n) {
    if (n.type().isFilter() && n.successor().type().isFilter()) return emptyList();

    final List<PlanNode> transformed = new MinCostList();

    final OptGroup<String> group = memo.get(n);
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

        // If the `newNode` has been bound with a group, then no need to further optimize it.
        // (because it must either have been or is being optimized.)
        if (memo.get(newNode) == null) transformed.add(newNode);
        group.add(newNode);
      }
    }

    transformed.addAll(listFlatMap(this::transform, transformed));
    return transformed;
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

  private static List<Match> match(PlanNode node, Operator op, Interpretations baseInter) {
    final List<Match> ret = new ArrayList<>();

    for (PlanNode n : Hint.apply(node, op, baseInter)) {
      // setup a new Interpretations to isolate each possibility
      final Interpretations inter = derivedInterpretations(baseInter);

      if (!op.match(n, inter)) continue;

      final PlanNode[] nodePreds = n.predecessors();
      final Operator[] opPreds = op.predecessors();

      // Note:
      // 1. Each child may have multiple matches
      // 2. Following matches can be affected by previous matches.
      // Thus, `matches` remembers the match results so far, and is passed to next match attempt.
      List<Match> matches = singletonList(Match.make(n, inter));
      for (int i = 0, bound = opPreds.length; i < bound; i++) {
        final PlanNode nextNode = nodePreds[i];
        final Operator nextOp = opPreds[i];

        matches = listFlatMap(it -> match(nextNode, nextOp, it.assignments()), matches);
        if (matches.isEmpty()) break;
        // lift the match point
        matches = listMap(Match::lift, matches);
      }

      ret.addAll(matches);
    }

    return ret;
  }
}
