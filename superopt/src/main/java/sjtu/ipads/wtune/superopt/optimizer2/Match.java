package sjtu.ipads.wtune.superopt.optimizer2;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.ListSupport.flatMap;
import static sjtu.ipads.wtune.common.utils.ListSupport.linkedListFlatMap;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;
import static sjtu.ipads.wtune.superopt.optimizer2.OptimizerSupport.FAILURE_UNKNOWN_OP;
import static sjtu.ipads.wtune.superopt.optimizer2.OptimizerSupport.predecessorOfFilters;

class Match {
  private final Substitution rule;
  private final Model model;

  private PlanContext sourcePlan;
  private int matchRootNode;

  private int lastMatchedNode;
  private Op lastMatchOp;

  private PlanContext modifiedPlan;
  private int result;

  Match(Substitution rule) {
    this.rule = rule;
    this.model = new Model(rule.constraints());
  }

  Match(Match other) {
    this.rule = other.rule;
    this.model = other.model.derive();
    this.sourcePlan = other.sourcePlan;
    this.matchRootNode = other.matchRootNode;
    this.lastMatchedNode = other.lastMatchedNode;
    this.lastMatchOp = other.lastMatchOp;
    this.modifiedPlan = other.modifiedPlan;
    this.result = other.result;
  }

  Match setSourcePlan(PlanContext sourcePlan) {
    this.sourcePlan = sourcePlan;
    this.model.setPlan(sourcePlan);
    return this;
  }

  Match setMatchRootNode(int nodeId) {
    this.matchRootNode = nodeId;
    return this;
  }

  Match setLastMatchPoint(int lastMatchedNode, Op lastMatchOp) {
    this.lastMatchedNode = lastMatchedNode;
    this.lastMatchOp = lastMatchOp;
    return this;
  }

  PlanContext sourcePlan() {
    return sourcePlan;
  }

  Model model() {
    return model;
  }

  int lastFailure() {
    return result;
  }

  boolean mkModifiedPlan() {
    final Instantiation instantiation = new Instantiation(rule, model);
    result = instantiation.instantiate();
    if (result <= 0) return false;

    modifiedPlan = instantiation.instantiatedPlan();

    final int parentId = sourcePlan.parentOf(matchRootNode);
    final int newRootId;
    if (parentId == NO_SUCH_NODE) newRootId = result;
    else {
      final int[] children = sourcePlan.childrenOf(sourcePlan.parentOf(matchRootNode));
      final int childIdx = children[0] == matchRootNode ? 0 : 1;
      modifiedPlan.setChild(sourcePlan.parentOf(matchRootNode), childIdx, result);
      newRootId = sourcePlan.root();
    }

    modifiedPlan.deleteDetached(newRootId);
    modifiedPlan.compact();
    return true;
  }

  int modifiedPoint() {
    return result;
  }

  PlanContext modifiedPlan() {
    return modifiedPlan;
  }

  Match derive() {
    return new Match(this);
  }

  boolean matchOne(Op op, int nodeId) {
    switch (op.kind()) {
      case INPUT:
        return matchInput((Input) op, nodeId);
      case INNER_JOIN:
      case LEFT_JOIN:
        return matchJoin((Join) op, nodeId);
      case SIMPLE_FILTER:
        return matchFilter((SimpleFilter) op, nodeId);
      case IN_SUB_FILTER:
        return matchInSub((InSubFilter) op, nodeId);
      case PROJ:
        return matchProj((Proj) op, nodeId);
      default:
        throw new IllegalArgumentException("unknown operator: " + op.kind());
    }
  }

  private boolean matchInput(Input input, int nodeId) {
    return model.assign(input.table(), nodeId) && model.checkConstraints();
  }

  private boolean matchJoin(Join joinOp, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.Join) return false;

    final OperatorType opKind = joinOp.kind();
    final InfoCache infoCache = sourcePlan.infoCache();
    final JoinNode joinNode = (JoinNode) sourcePlan.nodeAt(nodeId);

    if (opKind == LEFT_JOIN && joinNode.joinKind() != JoinKind.LEFT_JOIN) return false;
    if (opKind == INNER_JOIN && joinNode.joinKind() != JoinKind.INNER_JOIN) return false;
    if (!infoCache.isEquiJoin(nodeId)) return false;

    final var keys = infoCache.getJoinKeyOf(nodeId);
    return model.assign(joinOp.lhsAttrs(), keys.getLeft())
        && model.assign(joinOp.rhsAttrs(), keys.getRight())
        && model.checkConstraints();
  }

  private boolean matchFilter(SimpleFilter filter, int nodeId) {
    final PlanKind nodeKind = sourcePlan.kindOf(nodeId);
    if (!nodeKind.isFilter()) return false;

    final Expression predicate;
    if (nodeKind.isSubqueryFilter()) predicate = sourcePlan.infoCache().getSubqueryExprOf(nodeId);
    else predicate = ((SimpleFilterNode) sourcePlan.nodeAt(nodeId)).predicate();

    final Values attrs = sourcePlan.valuesReg().valueRefsOf(predicate);

    return model.assign(filter.predicate(), predicate)
        && model.assign(filter.attrs(), attrs)
        && model.checkConstraints();
  }

  private boolean matchInSub(InSubFilter inSub, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.InSub) return false;

    final InSubNode inSubNode = (InSubNode) sourcePlan.nodeAt(nodeId);
    if (!inSubNode.isPlain()) return false;

    final Expression expr = inSubNode.expr();
    final Values attrs = sourcePlan.valuesReg().valueRefsOf(expr);

    return model.assign(inSub.attrs(), attrs) && model.checkConstraints();
  }

  private boolean matchProj(Proj proj, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.Proj) return false;

    final ProjNode projNode = (ProjNode) sourcePlan.nodeAt(nodeId);
    if (proj.isDeduplicated() ^ projNode.deduplicated()) return false;

    final ValuesRegistry valuesReg = sourcePlan.valuesReg();
    final List<Value> outValues = valuesReg.valuesOf(nodeId);
    final List<Value> inValues = flatMap(projNode.attrExprs(), valuesReg::valueRefsOf);

    return model.assign(proj.attrs(), inValues)
        && model.assign(proj.schema(), outValues)
        && model.checkConstraints();
  }

  static List<Match> match(Match match, Op op, int nodeId) {
    if (op.kind() == INPUT || op.kind() == PROJ) {
      if (match.matchOne(op, nodeId)) return singletonList(match);
      else return emptyList();
    }

    final PlanContext plan = match.sourcePlan();
    if (op.kind().isJoin()) {
      if (plan.kindOf(nodeId) != PlanKind.Join) return emptyList();

      final Op nextOp0 = op.predecessors()[0], nextOp1 = op.predecessors()[1];
      final int nextNode0 = plan.childOf(nodeId, 0), nextNode1 = plan.childOf(nodeId, 1);

      final JoinMatcher matcher = new JoinMatcher((Join) op, plan, nodeId);
      List<Match> matches = matcher.matchBasedOn(match);
      matches = linkedListFlatMap(matches, m -> match(m, nextOp0, nextNode0));
      matches = linkedListFlatMap(matches, m -> match(m, nextOp1, nextNode1));
      return matches;
    }

    if (op.kind().isFilter()) {
      // Matching filter is holistic: the whole filter chain are matched as a whole. Thus the
      // matching happens only at the chain's head.
      assert op.successor() == null || !op.successor().kind().isFilter();
      if (!plan.kindOf(nodeId).isFilter()) return emptyList();

      final FilterMatcher matcher = new FilterMatcher((Filter) op, plan, nodeId);
      final List<Match> matches = matcher.matchBasedOn(match);
      final Op nextOp = predecessorOfFilters(op);
      return linkedListFlatMap(matches, m -> match(m, nextOp, plan.childOf(m.lastMatchedNode, 0)));
    }

    OptimizerSupport.setLastError(FAILURE_UNKNOWN_OP + op.kind());
    return emptyList();
  }
}
