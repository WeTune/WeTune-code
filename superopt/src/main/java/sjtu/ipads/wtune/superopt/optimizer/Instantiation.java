package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.Commons.dumpException;
import static sjtu.ipads.wtune.common.utils.IterableSupport.linearFind;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.tryResolveRef;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.INNER_JOIN;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.*;

class Instantiation {
  private final Substitution rule;
  private final Model model;
  private final PlanContext newPlan;

  private String error;

  Instantiation(Substitution rule, Model model) {
    this.rule = rule;
    this.model = model;
    this.newPlan = model.plan().copy();
  }

  PlanContext instantiatedPlan() {
    return newPlan;
  }

  int instantiate() {
    try {
      return instantiate(rule._1().root());
    } catch (Throwable ex) {
      error = dumpException(ex);
      return NO_SUCH_NODE;
    }
  }

  String lastError() {
    return error;
  }

  private int instantiate(Op op) {
    final OpKind kind = op.kind();
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

    newPlan.detachNode(nodeId);
    return nodeId;
  }

  private int instantiateJoin(Join join) {
    final int lhs = instantiate(join.predecessors()[0]);
    final int rhs = instantiate(join.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    List<Value> lhsKeys = model.ofAttrs(instantiationOf(join.lhsAttrs()));
    List<Value> rhsKeys = model.ofAttrs(instantiationOf(join.rhsAttrs()));

    if (lhsKeys == null || rhsKeys == null) return fail(FAILURE_INCOMPLETE_MODEL);
    if (lhsKeys.size() != rhsKeys.size() || lhsKeys.isEmpty())
      return fail(FAILURE_MISMATCHED_JOIN_KEYS);

    lhsKeys = adaptValues(lhsKeys, outValuesOf(lhs));
    rhsKeys = adaptValues(rhsKeys, outValuesOf(rhs));
    if (lhsKeys == null || rhsKeys == null) return fail(FAILURE_FOREIGN_VALUE);

    final JoinKind joinKind = join.kind() == INNER_JOIN ? JoinKind.INNER_JOIN : JoinKind.LEFT_JOIN;
    final Expression joinCond = PlanSupport.mkJoinCond(lhsKeys.size());
    final JoinNode joinNode = JoinNode.mk(joinKind, joinCond);
    final int joinNodeId = newPlan.bindNode(joinNode);

    newPlan.setChild(joinNodeId, 0, lhs);
    newPlan.setChild(joinNodeId, 1, rhs);
    newPlan.valuesReg().bindValueRefs(joinCond, interleaveJoinKeys(lhsKeys, rhsKeys));
    newPlan.infoCache().putJoinKeyOf(joinNodeId, lhsKeys, rhsKeys);

    return joinNodeId;
  }

  private int instantiateFilter(SimpleFilter filter) {
    final int child = instantiate(filter.predecessors()[0]);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final Expression predicate = model.ofPred(instantiationOf(filter.predicate()));
    List<Value> values = model.ofAttrs(instantiationOf(filter.attrs()));
    if (predicate == null || values == null || values.isEmpty())
      return fail(FAILURE_INCOMPLETE_MODEL);

    values = adaptValues(values, outValuesOf(child));
    if (values == null) return fail(FAILURE_FOREIGN_VALUE);

    return mkFilterNode(predicate, values, child);
  }

  private int instantiateInSub(InSubFilter inSub) {
    final int lhs = instantiate(inSub.predecessors()[0]);
    final int rhs = instantiate(inSub.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    List<Value> values = model.ofAttrs(instantiationOf(inSub.attrs()));
    if (values == null || values.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);

    values = adaptValues(values, outValuesOf(lhs));
    if (values == null) return fail(FAILURE_FOREIGN_VALUE);

    final Expression expression = PlanSupport.mkColRefsExpr(values.size());
    final InSubNode inSubNode = InSubNode.mk(expression);
    final int inSubNodeId = newPlan.bindNode(inSubNode);

    newPlan.setChild(inSubNodeId, 0, lhs);
    newPlan.setChild(inSubNodeId, 1, rhs);
    newPlan.valuesReg().bindValueRefs(expression, values);
    return inSubNodeId;
  }

  private int instantiateProj(Proj proj) {
    final int child = instantiate(proj.predecessors()[0]);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> outAttrs = model.ofSchema(instantiationOf(proj.schema()));
    if (outAttrs == null || outAttrs.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);

    List<Value> inAttrs = model.ofAttrs(instantiationOf(proj.attrs()));
    if (inAttrs == null || inAttrs.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);

    inAttrs = adaptValues(inAttrs, outValuesOf(child));
    if (inAttrs == null) return fail(FAILURE_FOREIGN_VALUE);

    final ValuesRegistry valuesReg = newPlan.valuesReg();
    final List<String> names = ListSupport.map(outAttrs, Value::name);
    final List<Expression> exprs = ListSupport.map(outAttrs, valuesReg::exprOf);

    final ProjNode projNode = ProjNode.mk(proj.isDeduplicated(), names, exprs);
    final int projNodeId = newPlan.bindNode(projNode);
    newPlan.setChild(projNodeId, 0, child);

    int offset = 0;
    for (Expression expr : exprs) {
      final int numRefs = expr.colRefs().size();
      valuesReg.bindValueRefs(expr, new ArrayList<>(inAttrs.subList(offset, offset + numRefs)));
      offset += numRefs;
    }
    valuesReg.bindValues(projNodeId, outAttrs);

    return projNodeId;
  }

  private int fail(String reason) {
    error = reason;
    return NO_SUCH_NODE;
  }

  private Symbol instantiationOf(Symbol symbol) {
    return rule.constraints().instantiationOf(symbol);
  }

  private List<Value> outValuesOf(int nodeId) {
    return newPlan.valuesReg().valuesOf(nodeId);
  }

  private List<Value> adaptValues(List<Value> values, List<Value> inValues) {
    // Handle subquery elimination.
    // e.g., Select sub.a From (Select t.a, t.b From t) As sub
    //       -> Select sub.a From t
    // But "sub.a" is actually not present in the out-values of "(Select t.a, t.b From t) As sub".
    // We have to trace the ref-chain of "sub.a", and find that "t.a" is present.
    // Finally, we replace "sub.a" by "t.a".
    List<Value> adaptedValues = null;
    for (int i = 0, bound = values.size(); i < bound; i++) {
      final Value adapted = adaptValue(inValues.get(i), inValues);
      if (adapted == null) return null;
      if (adapted != inValues.get(i)) {
        if (adaptedValues == null) adaptedValues = new ArrayList<>(values.size());
        adaptedValues.add(adapted);
      }
    }
    return adaptedValues == null ? values : adaptedValues;
  }

  private Value adaptValue(Value value, List<Value> lookup) {
    final List<Value> refChain = new ArrayList<>(5);
    for (; value != null; value = tryResolveRef(newPlan, value)) refChain.add(value);
    return linearFind(lookup, refChain::contains);
  }

  private int mkFilterNode(Expression expr, List<Value> refs, int child) {
    final InfoCache infoCache = model.plan().infoCache();
    final ValuesRegistry valuesReg = model.plan().valuesReg();

    final int subqueryNode = infoCache.getSubqueryNodeOf(expr);
    if (subqueryNode != NO_SUCH_NODE) {
      final Expression inSubExpr = getFilterExpr(subqueryNode);
      newPlan.detachNode(subqueryNode);
      newPlan.setChild(subqueryNode, 0, child);
      valuesReg.bindValueRefs(inSubExpr, refs);
      return subqueryNode;
    }

    final int[] components = infoCache.getVirtualExprComponents(expr);
    if (components != null) {
      int offset = 0;

      newPlan.setChild(components[0], 0, child);
      for (int i = 0, bound = components.length; i < bound; ++i) {
        final Expression filterExpr = getFilterExpr(components[i]);
        final int numRefs = filterExpr.colRefs().size();
        valuesReg.bindValueRefs(expr, new ArrayList<>(refs.subList(offset, offset + numRefs)));
        offset += numRefs;

        if (i > 0) newPlan.setChild(components[i], 0, components[i - 1]);
        newPlan.detachNode(components[i]);
      }

      return components[components.length - 1];
    }

    final SimpleFilterNode filterNode = SimpleFilterNode.mk(expr);
    final int filterNodeId = newPlan.bindNode(filterNode);

    newPlan.setChild(filterNodeId, 0, child);
    newPlan.valuesReg().bindValueRefs(expr, refs);
    return filterNodeId;
  }

  private Expression getFilterExpr(int nodeId) {
    final PlanNode node = newPlan.nodeAt(nodeId);
    final PlanKind nodeKind = node.kind();
    if (nodeKind == PlanKind.Filter) return ((SimpleFilterNode) node).predicate();
    else if (nodeKind == PlanKind.InSub) return ((InSubNode) node).expr();
    else return null;
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
