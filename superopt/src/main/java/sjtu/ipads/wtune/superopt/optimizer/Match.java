package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.ast.constants.JoinKind;
import sjtu.ipads.wtune.sql.plan.*;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.tree.TreeSupport.indexOfChild;
import static sjtu.ipads.wtune.common.tree.TreeSupport.rootOf;
import static sjtu.ipads.wtune.common.utils.ListSupport.flatMap;
import static sjtu.ipads.wtune.common.utils.ListSupport.linkedListFlatMap;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.joinKindOf;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.stringifyTree;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.*;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.FAILURE_UNKNOWN_OP;

class Match {
  private final Substitution rule;
  private final Model model;

  private PlanContext sourcePlan;
  private int matchRootNode;

  private int lastMatchedNode;
  private Op lastMatchedOp;

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
    this.lastMatchedOp = other.lastMatchedOp;
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
    this.lastMatchedOp = lastMatchOp;
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
    final int modifiedPointId = instantiation.instantiate();
    if (modifiedPointId <= 0) {
      OptimizerSupport.setLastError(instantiation.lastError());
      return false;
    }

    modifiedPlan = instantiation.instantiatedPlan();
    final PlanNode modifiedNode = modifiedPlan.nodeAt(modifiedPointId);
    System.out.println(stringifyTree(modifiedPlan, rootOf(modifiedPlan, modifiedPointId)));

    final int parentId = sourcePlan.parentOf(matchRootNode);
    final int newRootId;
    if (parentId == NO_SUCH_NODE) newRootId = modifiedPointId;
    else {
      final int childIdx = indexOfChild(sourcePlan, matchRootNode);
      modifiedPlan.setChild(sourcePlan.parentOf(matchRootNode), childIdx, modifiedPointId);
      newRootId = sourcePlan.root();
    }

    modifiedPlan.deleteDetached(newRootId);
    modifiedPlan.compact();
    result = modifiedPlan.nodeIdOf(modifiedNode);
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
    boolean result;
    switch (op.kind()) {
      case INPUT:
        result = matchInput((Input) op, nodeId);
        break;
      case INNER_JOIN:
      case LEFT_JOIN:
        result = matchJoin((Join) op, nodeId);
        break;
      case SIMPLE_FILTER:
        result = matchFilter((SimpleFilter) op, nodeId);
        break;
      case IN_SUB_FILTER:
        result = matchInSub((InSubFilter) op, nodeId);
        break;
      case PROJ:
        result = matchProj((Proj) op, nodeId);
        break;
      default:
        throw new IllegalArgumentException("unknown operator: " + op.kind());
    }

    if (result) setLastMatchPoint(nodeId, op);
    return result;
  }

  private boolean matchInput(Input input, int nodeId) {
    return model.assign(input.table(), nodeId) && model.checkConstraints();
  }

  private boolean matchJoin(Join joinOp, int nodeId) {
    if (sourcePlan.kindOf(nodeId) != PlanKind.Join) return false;

    final OpKind opKind = joinOp.kind();
    final InfoCache infoCache = sourcePlan.infoCache();
    final JoinKind joinKind = joinKindOf(sourcePlan, nodeId);
    if (opKind == LEFT_JOIN && joinKind != JoinKind.LEFT_JOIN) return false;
    if (opKind == INNER_JOIN && joinKind != JoinKind.INNER_JOIN) return false;
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

  private Op nextOp(int childIdx) {
    return lastMatchedOp.predecessors()[childIdx];
  }

  private int nextNode(int childIdx) {
    return sourcePlan.childOf(lastMatchedNode, childIdx);
  }

  static List<Match> match(Match match, Op op, int nodeId) {
    if (op.kind() == INPUT) {
      if (match.matchOne(op, nodeId)) return singletonList(match);
      else return emptyList();
    }

    final PlanContext plan = match.sourcePlan();
    if (op.kind() == PROJ) {
      if (match.matchOne(op, nodeId))
        return match(match, op.predecessors()[0], plan.childOf(nodeId, 0));
      else return emptyList();
    }

    if (op.kind().isJoin()) {
      if (plan.kindOf(nodeId) != PlanKind.Join) return emptyList();

      final JoinMatcher matcher = new JoinMatcher((Join) op, plan, nodeId);
      final List<Match> localMatches = matcher.matchBasedOn(match);
      List<Match> matches = new LinkedList<>();
      for (Match localMatch : localMatches) {
        final Op nextOp0 = localMatch.nextOp(0), nextOp1 = localMatch.nextOp(1);
        final int nextNode0 = localMatch.nextNode(0), nextNode1 = localMatch.nextNode(1);
        final List<Match> partialMatches = match(localMatch, nextOp0, nextNode0);
        matches.addAll(linkedListFlatMap(partialMatches, m -> match(m, nextOp1, nextNode1)));
      }

      return matches;
    }

    if (op.kind().isFilter()) {
      // Matching filter is holistic: the whole filter chain are matched as a whole. Thus the
      // matching happens only at the chain's head.
      assert op.successor() == null || !op.successor().kind().isFilter();
      if (!plan.kindOf(nodeId).isFilter()) return emptyList();

      final FilterMatcher matcher = new FilterMatcher((Filter) op, plan, nodeId);
      final List<Match> matches = matcher.matchBasedOn(match);
      return linkedListFlatMap(matches, m -> match(m, m.nextOp(0), m.nextNode(0)));
    }

    OptimizerSupport.setLastError(FAILURE_UNKNOWN_OP + op.kind());
    return emptyList();
  }
}
