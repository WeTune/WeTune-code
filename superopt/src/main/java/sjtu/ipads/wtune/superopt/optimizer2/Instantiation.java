package sjtu.ipads.wtune.superopt.optimizer2;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.IterableSupport.linearFind;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.tryResolveRef;
import static sjtu.ipads.wtune.superopt.optimizer2.OptimizerSupport.*;

class Instantiation {
  private final Substitution rule;
  private final Model model;
  private final PlanContext newPlan;

  private int failure;

  Instantiation(Substitution rule, Model model) {
    this.rule = rule;
    this.model = model;
    this.newPlan = model.plan().copy();
  }

  int instantiate(Op op) {
    final OperatorType kind = op.kind();
    switch (kind) {
      case INPUT:
        return instantiateInput((Input) op);
      case INNER_JOIN:
      case LEFT_JOIN:
        return instantiateJoin((Join) op);
      case SIMPLE_FILTER:
        return instantiateFilter((SimpleFilter) op);
      case IN_SUB_FILTER:
        return instantiateInSub((InSubFilter) op);
      case PROJ:
        return instantiateProj((Proj) op);
      default:
        return fail(FAILURE_UNKNOWN_OP);
    }
  }

  private int instantiateInput(Input input) {
    final Integer nodeId = model.ofTable(instantiationOf(input.table()));
    if (nodeId == null) return fail(FAILURE_INCOMPLETE_MODEL);
    else return nodeId;
  }

  private int instantiateJoin(Join join) {
    final int lhs = instantiate(join.predecessors()[0]);
    final int rhs = instantiate(join.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> lhsJoinKeys = model.ofAttrs(instantiationOf(join.lhsAttrs()));
    final List<Value> rhsJoinKeys = model.ofAttrs(instantiationOf(join.rhsAttrs()));
    if (lhsJoinKeys == null || rhsJoinKeys == null) return fail(FAILURE_INCOMPLETE_MODEL);
    if (lhsJoinKeys.size() != rhsJoinKeys.size() || lhsJoinKeys.isEmpty())
      return fail(FAILURE_MISMATCHED_JOIN_KEYS);
    if (!adaptValues(lhsJoinKeys, valuesOf(lhs)) || !adaptValues(rhsJoinKeys, valuesOf(rhs)))
      return fail(FAILURE_FOREIGN_VALUE);

    final JoinKind joinKind = join.kind() == INNER_JOIN ? JoinKind.INNER_JOIN : JoinKind.LEFT_JOIN;
    final Expression joinCond = PlanSupport.mkJoinCond(lhsJoinKeys.size());
    final JoinNode joinNode = JoinNode.mk(joinKind, joinCond);
    final int joinNodeId = newPlan.bindNode(joinNode);

    newPlan.valuesReg().bindValueRefs(joinCond, interleaveJoinKeys(lhsJoinKeys, rhsJoinKeys));

    return joinNodeId;
  }

  private int instantiateFilter(SimpleFilter filter) {
    final int child = instantiate(filter);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final Expression predicate = model.ofPred(instantiationOf(filter.predicate()));
    final List<Value> values = model.ofAttrs(instantiationOf(filter.attrs()));
    if (predicate == null || values == null || values.isEmpty())
      return fail(FAILURE_INCOMPLETE_MODEL);
    if (!adaptValues(values, valuesOf(child))) return fail(FAILURE_FOREIGN_VALUE);

    final SimpleFilterNode filterNode = SimpleFilterNode.mk(predicate);
    final int filterNodeId = newPlan.bindNode(filterNode);

    newPlan.valuesReg().bindValueRefs(predicate, values);
    return filterNodeId;
  }

  private int instantiateInSub(InSubFilter inSub) {
    final int child = instantiate(inSub);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> values = model.ofAttrs(instantiationOf(inSub.attrs()));
    if (values == null || values.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);
    if (!adaptValues(values, valuesOf(child))) return fail(FAILURE_FOREIGN_VALUE);

    final Expression expression = PlanSupport.mkColRefsExpr(values.size());
    final InSubNode inSubNode = InSubNode.mk(expression);
    final int inSubNodeId = newPlan.bindNode(inSubNode);

    newPlan.valuesReg().bindValueRefs(expression, values);
    return inSubNodeId;
  }

  private int instantiateProj(Proj proj) {
    final int child = instantiate(proj);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> values = model.ofAttrs(instantiationOf(proj.attrs()));
    if (values == null || values.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);

    final int initiator = newPlan.valuesReg().initiatorOf(values.get(0));
    if (newPlan.kindOf(initiator) != PlanKind.Proj) return fail(FAILURE_BAD_PROJECTION);

    return initiator;
  }

  private int fail(int reason) {
    failure = reason;
    return NO_SUCH_NODE;
  }

  private Symbol instantiationOf(Symbol symbol) {
    return rule.constraints().instantiationOf(symbol);
  }

  private List<Value> valuesOf(int nodeId) {
    return newPlan.valuesReg().valuesOf(nodeId);
  }

  private boolean adaptValues(List<Value> values, List<Value> inValues) {
    for (int i = 0, bound = values.size(); i < bound; i++) {
      final Value adapted = adaptValue(inValues.get(i), inValues);
      if (adapted == null) return false;
      inValues.set(i, adapted);
    }
    return true;
  }

  private Value adaptValue(Value value, List<Value> lookup) {
    final List<Value> refChain = new ArrayList<>(5);
    for (; value != null; value = tryResolveRef(newPlan, value)) refChain.add(value);
    return linearFind(lookup, refChain::contains);
  }

  private static List<Value> interleaveJoinKeys(List<Value> lhsJoinKeys, List<Value> rhsJoinKeys) {
    final List<Value> joinKeys = new ArrayList<>(lhsJoinKeys.size() << 1);
    for (int i = 0, bound = lhsJoinKeys.size(); i < bound; i++) {
      joinKeys.add(lhsJoinKeys.get(i));
      joinKeys.add(rhsJoinKeys.get(i));
    }
    return joinKeys;
  }
}
