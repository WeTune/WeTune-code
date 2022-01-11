package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ast.constants.JoinKind;
import sjtu.ipads.wtune.sql.ast.constants.SetOpKind;
import sjtu.ipads.wtune.sql.plan.*;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.Commons.dumpException;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.INNER_JOIN;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.*;

class Instantiation {
  private final Substitution rule;
  private final Model model;
  private final PlanContext newPlan;
  private final ValueRefReBinder reBinder;

  private String error;

  Instantiation(Substitution rule, Model model) {
    this.rule = rule;
    this.model = model;
    this.newPlan = model.plan().copy();
    this.reBinder = new ValueRefReBinder(newPlan);
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
      case SET_OP:
        return instantiateSetOp((Union) op);
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

    lhsKeys = reBinder.rebindRefs(lhsKeys, outValuesOf(lhs));
    rhsKeys = reBinder.rebindRefs(rhsKeys, outValuesOf(rhs));
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

    values = reBinder.rebindRefs(values, outValuesOf(child));
    if (values == null) return fail(FAILURE_FOREIGN_VALUE);

    return mkFilterNode(predicate, values, child);
  }

  private int instantiateInSub(InSubFilter inSub) {
    final int lhs = instantiate(inSub.predecessors()[0]);
    final int rhs = instantiate(inSub.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    List<Value> values = model.ofAttrs(instantiationOf(inSub.attrs()));
    if (values == null || values.isEmpty()) return fail(FAILURE_INCOMPLETE_MODEL);

    values = reBinder.rebindRefs(values, outValuesOf(lhs));
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
    if (outAttrs == null) return fail(FAILURE_INCOMPLETE_MODEL);

    List<Value> inAttrs = model.ofAttrs(instantiationOf(proj.attrs()));
    if (inAttrs == null) return fail(FAILURE_INCOMPLETE_MODEL);

    inAttrs = reBinder.rebindRefs(inAttrs, outValuesOf(child));
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

  private int instantiateSetOp(Union union) {
    final int lhs = instantiate(union.predecessors()[0]);
    final int rhs = instantiate(union.predecessors()[1]);
    if (lhs == NO_SUCH_NODE || rhs == NO_SUCH_NODE) return NO_SUCH_NODE;

    final SetOpNode unionNode = SetOpNode.mk(union.isDeduplicated(), SetOpKind.UNION);
    final int unionNodeId = newPlan.bindNode(unionNode);
    newPlan.setChild(unionNodeId, 0, lhs);
    newPlan.setChild(unionNodeId, 1, rhs);

    return unionNodeId;
  }

  private int instantiateAgg(Agg agg) {
    final int child = instantiate(agg.predecessors()[0]);
    if (child == NO_SUCH_NODE) return NO_SUCH_NODE;

    final List<Value> outValues = null; // TODO
    if (outValues == null) return fail(FAILURE_INCOMPLETE_MODEL);

    List<Value> aggRefs = model.ofAttrs(instantiationOf(agg.aggregateAttrs()));
    List<Value> groupRefs = model.ofAttrs(instantiationOf(agg.groupByAttrs()));
    final Expression aggFunc = model.ofFunc(instantiationOf(agg.aggFunc()));
    final Expression havingPred = model.ofPred(instantiationOf(agg.havingPred()));
    if (aggRefs == null || groupRefs == null || aggFunc == null)
      return fail(FAILURE_INCOMPLETE_MODEL);

    final List<Value> inValues = outValuesOf(child);

    aggRefs = reBinder.rebindRefs(aggRefs, inValues);
    if (aggRefs == null) return fail(FAILURE_FOREIGN_VALUE);

    groupRefs = reBinder.rebindRefs(groupRefs, ListSupport.join(outValues, inValues));
    if (groupRefs == null) return fail(FAILURE_FOREIGN_VALUE);

    final ValuesRegistry valuesReg = newPlan.valuesReg();
    final List<String> names = ListSupport.map(outValues, Value::name);
    final List<Expression> aggExprs = ListSupport.map(outValues, valuesReg::exprOf);
    final List<Expression> groupExprs = ListSupport.map(groupRefs, PlanSupport::mkColRefExpr);

    final AggNode aggNode = AggNode.mk(false, names, aggExprs, groupExprs, havingPred);
    final int aggNodeId = newPlan.bindNode(aggNode);

    return aggNodeId; // TODO
    //
    //    final Expression havingPred = model.ofPred(instantiationOf(agg.havingPred()));
    //
    //    final List<String> names = ListSupport.map(outAttrs, Value::name);
    //    final List<Expression> exprs = ListSupport.map(outAttrs, valuesReg::exprOf);
    //
    //    final SetOpNode unionNode = SetOpNode.mk(union.isDeduplicated(), SetOpKind.UNION);
    //    final int unionNodeId = newPlan.bindNode(unionNode);
    //    newPlan.setChild(unionNodeId, 0, child);
    //    newPlan.setChild(unionNodeId, 1, rhs);
    //
    //    return unionNodeId;
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

  private int mkFilterNode(Expression expr, List<Value> refs, int child) {
    final InfoCache infoCache = model.plan().infoCache();

    final int subqueryNode = infoCache.getSubqueryNodeOf(expr);
    if (subqueryNode != NO_SUCH_NODE) {
      newPlan.detachNode(subqueryNode);
      newPlan.setChild(subqueryNode, 0, child);
      rebindFilterExpr(subqueryNode, refs);
      return subqueryNode;
    }

    final int[] components = infoCache.getVirtualExprComponents(expr);
    if (components != null) {
      final int total = refs.size();
      int offset = 0;

      newPlan.setChild(components[0], 0, child);
      for (int i = 0, bound = components.length; i < bound; ++i) {
        offset += rebindFilterExpr(components[i], refs.subList(offset, total));
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

  private int rebindFilterExpr(int nodeId, List<Value> refs) {
    final PlanKind kind = newPlan.kindOf(nodeId);
    final PlanNode node = newPlan.nodeAt(nodeId);
    final ValuesRegistry valuesReg = newPlan.valuesReg();

    final int numRefs;
    if (kind == PlanKind.Filter) {
      final Expression expr = ((SimpleFilterNode) node).predicate();
      numRefs = expr.colRefs().size();
      valuesReg.bindValueRefs(expr, new ArrayList<>(refs.subList(0, numRefs)));

    } else if (kind == PlanKind.InSub) {
      final Expression lhsExpr = ((InSubNode) node).expr();
      final Expression subqueryExpr = newPlan.infoCache().getSubqueryExprOf(nodeId);
      numRefs = subqueryExpr.colRefs().size();
      valuesReg.bindValueRefs(lhsExpr, new ArrayList<>(refs.subList(0, lhsExpr.colRefs().size())));
      valuesReg.bindValueRefs(subqueryExpr, new ArrayList<>(refs.subList(0, numRefs)));

    } else if (kind == PlanKind.Exists) {
      final Expression subqueryExpr = newPlan.infoCache().getSubqueryExprOf(nodeId);
      numRefs = subqueryExpr.colRefs().size();
      valuesReg.bindValueRefs(subqueryExpr, new ArrayList<>(refs.subList(0, numRefs)));

    } else {
      assert false;
      return -1;
    }

    return numRefs;
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
