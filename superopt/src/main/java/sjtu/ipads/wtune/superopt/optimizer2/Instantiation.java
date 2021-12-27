package sjtu.ipads.wtune.superopt.optimizer2;

import sjtu.ipads.wtune.common.utils.ListSupport;
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

  PlanContext instantiatedPlan() {
    return newPlan;
  }

  int instantiate() {
    final int rootId = instantiate(rule._1().root());
    return rootId == NO_SUCH_NODE ? failure : rootId;
  }

  int lastFailure() {
    return failure;
  }

  private int instantiate(Op op) {
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

    final List<Value> lhsKeys = model.ofAttrs(instantiationOf(join.lhsAttrs()));
    final List<Value> rhsKeys = model.ofAttrs(instantiationOf(join.rhsAttrs()));

    if (lhsKeys == null || rhsKeys == null) return fail(FAILURE_INCOMPLETE_MODEL);
    if (lhsKeys.size() != rhsKeys.size() || lhsKeys.isEmpty())
      return fail(FAILURE_MISMATCHED_JOIN_KEYS);
    if (!adaptValues(lhsKeys, outValuesOf(lhs)) || !adaptValues(rhsKeys, outValuesOf(rhs)))
      return fail(FAILURE_FOREIGN_VALUE);

    final JoinKind joinKind = join.kind() == INNER_JOIN ? JoinKind.INNER_JOIN : JoinKind.LEFT_JOIN;
    final Expression joinCond = PlanSupport.mkJoinCond(lhsKeys.size());
    final JoinNode joinNode = JoinNode.mk(joinKind, joinCond);
    final int joinNodeId = newPlan.bindNode(joinNode);

    newPlan.valuesReg().bindValueRefs(joinCond, interleaveJoinKeys(lhsKeys, rhsKeys));
    newPlan.infoCache().setJoinKeyOf(joinNodeId, lhsKeys, rhsKeys);

    return joinNodeId;
  }

  private int instantiateFilter(SimpleFilter filter) {
    final int child = instantiate(filter);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final Expression predicate = model.ofPred(instantiationOf(filter.predicate()));
    final List<Value> values = model.ofAttrs(instantiationOf(filter.attrs()));
    if (predicate == null || values == null || values.isEmpty())
      return fail(FAILURE_INCOMPLETE_MODEL);
    if (!adaptValues(values, outValuesOf(child))) return fail(FAILURE_FOREIGN_VALUE);

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
    if (!adaptValues(values, outValuesOf(child))) return fail(FAILURE_FOREIGN_VALUE);

    final Expression expression = PlanSupport.mkColRefsExpr(values.size());
    final InSubNode inSubNode = InSubNode.mk(expression);
    final int inSubNodeId = newPlan.bindNode(inSubNode);

    newPlan.valuesReg().bindValueRefs(expression, values);
    return inSubNodeId;
  }

  private int instantiateProj(Proj proj) {
    final int child = instantiate(proj);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> inAttrs = model.ofAttrs(instantiationOf(proj.attrs()));
    final List<Value> outAttrs = model.ofSchema(instantiationOf(proj.schema()));
    if (inAttrs == null || inAttrs.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);
    if (outAttrs == null || outAttrs.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);
    if (!adaptValues(inAttrs, outValuesOf(child))) return fail(FAILURE_FOREIGN_VALUE);

    final ValuesRegistry valuesReg = newPlan.valuesReg();
    final List<String> names = ListSupport.map(outAttrs, Value::name);
    final List<Expression> exprs = ListSupport.map(outAttrs, valuesReg::exprOf);

    final ProjNode projNode = ProjNode.mk(proj.isDeduplicated(), names, exprs);
    final int projNodeId = newPlan.bindNode(projNode);

    int i = 0;
    for (Expression expr : exprs) {
      final int numRefs = expr.colRefs().size();
      final ArrayList<Value> refs = new ArrayList<>(inAttrs.subList(i, i + numRefs));
      valuesReg.bindValueRefs(expr, refs);
      i += numRefs;
    }

    return projNodeId;
  }

  private int fail(int reason) {
    failure = reason;
    return NO_SUCH_NODE;
  }

  private Symbol instantiationOf(Symbol symbol) {
    return rule.constraints().instantiationOf(symbol);
  }

  private List<Value> outValuesOf(int nodeId) {
    return newPlan.valuesReg().valuesOf(nodeId);
  }

  private boolean adaptValues(List<Value> values, List<Value> inValues) {
    // Handle subquery elimination.
    // e.g., Select sub.a From (Select t.a, t.b From t) As sub
    //       -> Select sub.a From t
    // But "sub.a" is actually not present in the out-values of "(Select t.a, t.b From t) As sub".
    // We have to trace the ref-chain of "sub.a", and find that "t.a" is present.
    // Finally, we replace "sub.a" by "t.a".
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
