package sjtu.ipads.wtune.superopt.optimizer.support;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDef.locateDefiner;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PROJ;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedOnTree;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedToRoot;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.rootOf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan.SortNode;
import sjtu.ipads.wtune.sqlparser.plan.TypeBasedAlgorithm;
import sjtu.ipads.wtune.superopt.optimizer.join.JoinTree;

public class PlanNormalizer extends TypeBasedAlgorithm<PlanNode> {
  public static PlanNode normalize(PlanNode node) {
    if (node == null) return null;
    return new PlanNormalizer().dispatch(node);
  }

  @Override
  public PlanNode dispatch(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) if (dispatch(predecessor) == null) return null;
    return super.dispatch(node);
  }

  @Override
  protected PlanNode onInput(InputNode input) {
    return input;
  }

  @Override
  protected PlanNode onLimit(LimitNode limit) {
    return limit;
  }

  @Override
  protected PlanNode onSort(SortNode sort) {
    return sort;
  }

  @Override
  protected PlanNode onAgg(AggNode agg) {
    return agg;
  }

  @Override
  protected PlanNode onLeftJoin(JoinNode leftJoin) {
    final PlanNode successor = leftJoin.successor();
    if (successor.type().isJoin() && successor.predecessors()[1] == leftJoin) return null;
    return handleJoin(leftJoin);
  }

  @Override
  protected PlanNode onInnerJoin(JoinNode innerJoin) {
    return handleJoin(innerJoin);
  }

  @Override
  protected PlanNode onProj(ProjNode proj) {
    if (isRedundantProj(proj)) {
      proj.successor().replacePredecessor(proj, proj.predecessors()[0]);
      resolveUsedToRoot(proj.successor());
      return proj.predecessors()[0];
    }

    return proj;
  }

  @Override
  protected PlanNode onSubqueryFilter(FilterNode filter) {
    return filter;
  }

  @Override
  protected PlanNode onPlainFilter(FilterNode filter) {
    final List<FilterNode> chain = filter.breakDown();
    assert !chain.isEmpty();
    // memo: there is such situation: a subquery filter is coerced to a plain filter

    // link the chain
    filter.successor().replacePredecessor(filter, chain.get(0));
    for (int i = 0, bound = chain.size() - 1; i < bound; i++)
      chain.get(i).setPredecessor(0, chain.get(i + 1));
    tail(chain).setPredecessor(0, filter.predecessors()[0]);

    for (FilterNode f : chain)
      for (AttributeDef nonNullAttr : f.nonNullAttributes())
        enforceEffectiveNonNull(locateDefiner(nonNullAttr, f));

    return chain.get(0);
  }

  private static boolean isRedundantProj(ProjNode proj) {
    final PlanNode successor = proj.successor();
    final PlanNode predecessor = proj.predecessors()[0];

    return proj.isWildcard()
        && (successor instanceof JoinNode || successor instanceof ProjNode)
        && !predecessor.type().isFilter()
        && predecessor.type() != LEFT_JOIN;
  }

  private static void insertProj(PlanNode successor, PlanNode predecessor) {
    final ProjNode proj = ProjNode.makeWildcard(predecessor.definedAttributes());
    successor.replacePredecessor(predecessor, proj);
    proj.setPredecessor(0, predecessor);
  }

  private static void enforceEffectiveNonNull(PlanNode definer) {
    final PlanNode successor = definer.successor();
    if (successor.type() == LEFT_JOIN && successor.predecessors()[1] == definer)
      ((JoinNode) successor).setJoinType(INNER_JOIN);
  }

  private static PlanNode handleJoin(JoinNode node) {
    final PlanNode successor = node.successor();
    final PlanNode[] predecessors = node.predecessors();
    // 1. insert proj if a filter directly precedes a join
    final boolean insertProjOnLeft = predecessors[0].type().isFilter();
    final boolean insertProjOnRight = predecessors[1].type().isFilter();
    if (insertProjOnLeft) insertProj(node, predecessors[0]);
    if (insertProjOnRight) insertProj(node, predecessors[1]);
    if (insertProjOnLeft || insertProjOnRight) resolveUsedToRoot(node);

    // 2. enforce left-deep join tree
    node = enforceLeftDeepJoin(node);

    // extra operator at join root
    if (!successor.type().isJoin()) {
      // 3. rectify qualification
      //      resolveUsedOnTree(node); // necessary?
      if (rectifyQualification(node)) resolveUsedOnTree(rootOf(node));

      final JoinTree joinTree = JoinTree.make(node);
      node = joinTree.sorted().rebuild();
      successor.replacePredecessor(joinTree.originalRoot(), node);
    }
    return node;
  }

  private static JoinNode enforceLeftDeepJoin(JoinNode join) {
    final PlanNode successor = join.successor();
    final PlanNode right = join.predecessors()[1];
    assert right.type() != LEFT_JOIN;

    if (right.type() != INNER_JOIN) return join;

    final JoinNode newJoin = (JoinNode) right;

    final PlanNode b = right.predecessors()[0]; // b can be another JOIN
    final PlanNode c = right.predecessors()[1]; // c must not be a JOIN
    assert !c.type().isJoin();

    if (b.definedAttributes().containsAll(join.rightAttributes())) {
      // 1. join<a.x=b.y>(a,newJoin<b.z=c.w>(b,c)) => newJoin<b.z=c.w>(join<a.x=b.y>(a,b),c)
      join.setPredecessor(1, b);
      newJoin.setPredecessor(0, join);
      newJoin.setPredecessor(1, c);

      //      join.resolveUsed(); // necessary?
      //      newJoin.resolveUsed(); // necessary?

      successor.replacePredecessor(join, newJoin);
      enforceLeftDeepJoin(join);
      return newJoin;

    } else {
      // 2. join<a.x=c.y>(a,newJoin<b.z=c.w>(b,c)) => newJoin<b.z=c.w>(join<a.x=c.y>(a,c),b)
      join.setPredecessor(1, c);
      newJoin.setPredecessor(0, join);
      newJoin.setPredecessor(1, b);

      // CRITICAL: the side of children are swapped, must re-resolved here
      newJoin.resolveUsed();

      successor.replacePredecessor(join, newJoin);
      return enforceLeftDeepJoin(newJoin);
    }
  }

  private static boolean rectifyQualification(PlanNode node) {
    // target:
    // 1. add the qualification to an unqualified subquery
    // 2. change the qualification to ensure non-duplicated
    if (!node.type().isJoin()) return false;

    final PlanNode left = node.predecessors()[0], right = node.predecessors()[1];
    assert !right.type().isJoin() && !right.type().isFilter();

    final Map<String, PlanNode> qualified = new HashMap<>();
    final Set<PlanNode> unqualified = newIdentitySet();

    // Classify attributes by whether qualified, group unqualified ones by definer
    for (AttributeDef attr : listJoin(left.definedAttributes(), right.definedAttributes())) {
      final String qualification = attr.qualification();
      final PlanNode definer = locateDefiner(attr, node);
      if (qualification == null
          || qualified.compute(qualification, (s, n) -> coalesce(n, definer)) != definer)
        unqualified.add(definer);
    }

    if (unqualified.isEmpty()) return false;

    // set unique qualification to unqualified ones
    for (PlanNode n : unqualified) {
      final String qualification = makeQualification(qualified.keySet());
      setQualification(n, qualification);
      qualified.put(qualification, n);
    }
    return true;
  }

  private static String makeQualification(Set<String> existing) {
    int i = 0;
    while (true) {
      final String qualification = "q" + i;
      if (!existing.contains(qualification)) return qualification;
      ++i;
    }
  }

  private static void setQualification(PlanNode node, String qualification) {
    assert node.type() == PROJ || node.type() == INPUT;
    if (node.type() == INPUT) ((InputNode) node).setAlias(qualification);
    else if (node.type() == PROJ) ((ProjNode) node).setQualification(qualification);
  }
}
