package sjtu.ipads.wtune.superopt.optimizer.support;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.util.ASTHelper;
import sjtu.ipads.wtune.superopt.optimizer.join.JoinTree;

import java.util.*;

import static java.util.Collections.newSetFromMap;
import static sjtu.ipads.wtune.common.utils.Commons.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.func;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.IS;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedOnTree;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedToRoot;

public class PlanNormalizer extends TypeBasedAlgorithm<PlanNode> {

  public static PlanNode normalize(PlanNode node) {
    return new PlanNormalizer().dispatch(node);
  }

  @Override
  protected PlanNode dispatch(PlanNode node) {
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
  protected PlanNode onSubqueryFilter(SubqueryFilterNode filter) {
    filter.resolveUsed();
    return filter;
  }

  @Override
  protected PlanNode onLeftJoin(LeftJoinNode leftJoin) {
    final PlanNode successor = leftJoin.successor();
    if (successor.type().isJoin() && successor.predecessors()[1] == leftJoin) return null;
    return handleJoin(leftJoin);
  }

  @Override
  protected PlanNode onInnerJoin(InnerJoinNode innerJoin) {
    return handleJoin(innerJoin);
  }

  @Override
  protected PlanNode onProj(ProjNode proj) {
    if (isRedundantProj(proj)) {
      proj.successor().replacePredecessor(proj, proj.predecessors()[0]);
      resolveUsedToRoot(proj.successor());
      return proj.predecessors()[0];
    }
    proj.resolveUsed();
    return proj;
  }

  @Override
  protected PlanNode onPlainFilter(PlainFilterNode filter) {
    final List<FilterNode> filters = filter.breakDown();
    assert !filters.isEmpty();

    if (filters.size() != 1) {
      for (int i = 0, bound = filters.size() - 1; i < bound; i++)
        filters.get(i).setPredecessor(0, filters.get(i + 1));

      tail(filters).setPredecessor(0, filter.predecessors()[0]);
      filter.successor().replacePredecessor(filter, filters.get(0));

    } else assert filters.get(0) == filter;

    for (FilterNode f : filters) {
      f.resolveUsed();
      if (!allowNullValue(f.rawExpr()))
        f.usedAttributes().forEach(PlanNormalizer::enforceEffectiveNonNull);
    }

    return filters.get(0);
  }

  private static boolean isRedundantProj(ProjNode proj) {
    return proj.isWildcard()
        && (proj.successor() instanceof JoinNode || proj.successor() instanceof ProjNode)
        && !proj.predecessors()[0].type().isFilter();
  }

  private static void insertProj(PlanNode successor, PlanNode predecessor) {
    final List<ASTNode> exprs =
        listMap(
            func(AttributeDef::toColumnRef).andThen(ASTHelper::makeSelectItem),
            predecessor.definedAttributes());

    final ProjNode proj = ProjNode.make(null, exprs);
    successor.replacePredecessor(predecessor, proj);
    proj.setWildcard(true);
    proj.setPredecessor(0, predecessor);
  }

  private static boolean allowNullValue(ASTNode expr) {
    final BinaryOp op = expr.get(BINARY_OP);
    return op == null
        || (op == IS && expr.get(BINARY_RIGHT).get(LITERAL_TYPE) == LiteralType.NULL)
        || !op.isRelation();
  }

  private static void enforceEffectiveNonNull(AttributeDef attr) {
    final PlanNode definer = attr.definer();
    final PlanNode successor = definer.successor();
    if (successor.type() == LeftJoin && successor.predecessors()[1] == definer)
      enforceEffectiveInnerJoin(successor);
  }

  private static void enforceEffectiveInnerJoin(PlanNode leftJoin) {
    assert leftJoin.type() == LeftJoin;
    final JoinNode innerJoin = ((JoinNode) leftJoin).toInnerJoin();
    leftJoin.successor().replacePredecessor(leftJoin, innerJoin);
    innerJoin.setPredecessor(0, leftJoin.predecessors()[0]);
    innerJoin.setPredecessor(1, leftJoin.predecessors()[1]);
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

    // 3. rectify qualification
    if (!successor.type().isJoin()) {
      resolveUsedOnTree(node);
      rectifyQualification(node);

      final JoinTree joinTree = JoinTree.make(node);
      node = joinTree.sorted().rebuild();
      successor.replacePredecessor(joinTree.originalRoot(), node);
    }
    return node;
  }

  private static JoinNode enforceLeftDeepJoin(JoinNode join) {
    final PlanNode successor = join.successor();
    final PlanNode right = join.predecessors()[1];
    assert right.type() != LeftJoin;

    if (right.type() != InnerJoin) return join;

    final JoinNode newJoin = (JoinNode) right;

    final PlanNode b = right.predecessors()[0]; // b can be another JOIN
    final PlanNode c = right.predecessors()[1]; // c must not be a JOIN
    assert !c.type().isJoin();

    if (b.definedAttributes().containsAll(join.rightAttributes())) {
      // 1. join<a.x=b.y>(a,join<b.z=c.w>(b,c)) => join<b.z=c.w>(join<a.x=b.y>(a,b),c)
      join.setPredecessor(1, b);
      newJoin.setPredecessor(0, join);
      newJoin.setPredecessor(1, c);

      join.resolveUsed();
      newJoin.resolveUsed();

      successor.replacePredecessor(join, newJoin);
      enforceLeftDeepJoin(join);
      return newJoin;

    } else {
      // 2. join<a.x=c.y>(a,join<b.z=c.w>(b,c)) => join<b.z=c.w>(join<a.x=c.y>(a,c),b)
      join.setPredecessor(1, c);
      newJoin.setPredecessor(0, join);
      newJoin.setPredecessor(1, b);

      // CRITICAL: the side of children are swapped, must re-resolved here
      newJoin.resolveUsed();

      successor.replacePredecessor(join, newJoin);
      return enforceLeftDeepJoin(newJoin);
    }
  }

  private static void rectifyQualification(PlanNode node) {
    // 1. add qualification to subquery (if absent)
    // 2. add qualification to distinguish the table sources of same table
    if (!node.type().isJoin()) return;

    final PlanNode left = node.predecessors()[0], right = node.predecessors()[1];
    assert right.type() == Proj || right.type() == OperatorType.Input;

    final Map<String, PlanNode> qualified = new HashMap<>();
    final Set<PlanNode> unqualified = newSetFromMap(new IdentityHashMap<>());

    // Classify attributes by whether qualified, group unqualified ones by definer
    for (AttributeDef attr : listJoin(left.definedAttributes(), right.definedAttributes())) {
      final String qualification = attr.qualification();
      final PlanNode definer = attr.definer();
      if (qualification == null
          || qualified.compute(qualification, (s, n) -> coalesce(n, definer)) != definer)
        unqualified.add(definer);
    }

    // set unique qualification to qualified ones
    for (PlanNode n : unqualified) {
      final String qualification = makeQualification(qualified.keySet());
      setQualification(n, qualification);
      qualified.put(qualification, n);
    }
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
    assert node.type() == Proj || node.type() == OperatorType.Input;
    if (node.type() == OperatorType.Input) ((InputNode) node).setAlias(qualification);
    node.definedAttributes().forEach(it -> it.setQualification(qualification));
  }
}
