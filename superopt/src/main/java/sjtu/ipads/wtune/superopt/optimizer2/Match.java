package sjtu.ipads.wtune.superopt.optimizer2;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.ListSupport.flatMap;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LEFT_JOIN;

class Match {
  private final PlanContext patternPlan;
  private final int matchPoint;
  private final Substitution rule;
  private final Model model;

  private PlanContext modifiedPlan;
  private int result;

  Match(PlanContext plan, int matchPoint, Substitution rule) {
    this.patternPlan = plan;
    this.matchPoint = matchPoint;
    this.rule = rule;
    this.model = new Model(plan, rule.constraints());
  }

  Model model() {
    return model;
  }

  boolean match(Op op, int nodeId) {
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

  boolean mkTransformedPlan() {
    final Instantiation instantiation = new Instantiation(rule, model);
    result = instantiation.instantiate();
    if (result <= 0) return false;

    modifiedPlan = instantiation.instantiatedPlan();

    final int parentId = patternPlan.parentOf(matchPoint);
    final int newRootId;
    if (parentId == NO_SUCH_NODE) newRootId = result;
    else {
      final int[] children = patternPlan.childrenOf(patternPlan.parentOf(matchPoint));
      final int childIdx = children[0] == matchPoint ? 0 : 1;
      modifiedPlan.setChild(patternPlan.parentOf(matchPoint), childIdx, result);
      newRootId = patternPlan.root();
    }

    modifiedPlan.deleteDetached(newRootId);
    modifiedPlan.compact();
    return true;
  }

  int lastFailure() {
    return result;
  }

  int modifiedPoint() {
    return result;
  }

  PlanContext modifiedPlan() {
    return modifiedPlan;
  }

  private boolean matchInput(Input input, int nodeId) {
    return model.assign(input.table(), nodeId) && model.checkConstraints();
  }

  private boolean matchJoin(Join joinOp, int nodeId) {
    if (patternPlan.kindOf(nodeId) != PlanKind.Join) return false;

    final OperatorType opKind = joinOp.kind();
    final InfoCache infoCache = patternPlan.infoCache();
    final JoinNode joinNode = (JoinNode) patternPlan.nodeAt(nodeId);

    if (opKind == LEFT_JOIN && joinNode.joinKind() != JoinKind.LEFT_JOIN) return false;
    if (opKind == INNER_JOIN && joinNode.joinKind() != JoinKind.INNER_JOIN) return false;
    if (!infoCache.isEquiJoin(nodeId)) return false;

    final var keys = infoCache.joinKeyOf(nodeId);
    return model.assign(joinOp.lhsAttrs(), keys.getLeft())
        && model.assign(joinOp.rhsAttrs(), keys.getRight())
        && model.checkConstraints();
  }

  private boolean matchFilter(SimpleFilter filter, int nodeId) {
    if (patternPlan.kindOf(nodeId) != PlanKind.Filter) return false;

    final SimpleFilterNode filterNode = (SimpleFilterNode) patternPlan.nodeAt(nodeId);

    final Expression predicate = filterNode.predicate();
    final Values attrs = patternPlan.valuesReg().valueRefsOf(predicate);

    return model.assign(filter.predicate(), predicate)
        && model.assign(filter.attrs(), attrs)
        && model.checkConstraints();
  }

  private boolean matchInSub(InSubFilter inSub, int nodeId) {
    if (patternPlan.kindOf(nodeId) != PlanKind.InSub) return false;

    final InSubNode inSubNode = (InSubNode) patternPlan.nodeAt(nodeId);
    if (!inSubNode.isPlain()) return false;

    final Expression expr = inSubNode.expr();
    final Values attrs = patternPlan.valuesReg().valueRefsOf(expr);

    return model.assign(inSub.attrs(), attrs) && model.checkConstraints();
  }

  private boolean matchProj(Proj proj, int nodeId) {
    if (patternPlan.kindOf(nodeId) != PlanKind.Proj) return false;

    final ProjNode projNode = (ProjNode) patternPlan.nodeAt(nodeId);
    if (proj.isDeduplicated() ^ projNode.deduplicated()) return false;

    final ValuesRegistry valuesReg = patternPlan.valuesReg();
    final List<Value> outValues = valuesReg.valuesOf(nodeId);
    final List<Value> inValues = flatMap(projNode.attrExprs(), valuesReg::valueRefsOf);

    return model.assign(proj.attrs(), inValues)
        && model.assign(proj.schema(), outValues)
        && model.checkConstraints();
  }
}
