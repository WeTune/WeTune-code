package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.SqlSupport;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.ColRef_ColName;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.ColName_Col;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.ColName_Table;

class ValueRefBinder {
  private final PlanContext plan;
  private final ValuesRegistry valuesReg;

  ValueRefBinder(PlanContext plan) {
    this.plan = plan;
    this.valuesReg = plan.valuesReg();
  }

  void bind() {
    try {
      bind0(plan.root(), Collections.emptyList());
    } catch (RuntimeException ex) {
      if (ex instanceof PlanException) throw ex;
      else throw new PlanException(ex);
    }
  }

  private void bind0(int nodeId, List<Value> secondaryLookup) {
    if (nodeId == NO_SUCH_NODE) return;

    switch (plan.kindOf(nodeId)) {
      case Limit:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        break;
      case Sort:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bindSort(nodeId);
        break;
      case SetOp:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bind0(plan.childOf(nodeId, 1), secondaryLookup);
        break;
      case Join:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bind0(plan.childOf(nodeId, 1), secondaryLookup);
        bindJoin(nodeId);
        break;
      case Agg:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bindAgg(nodeId);
        break;
      case Proj:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bindProj(nodeId);
        break;
      case Filter:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bindFilter(nodeId, secondaryLookup);
        break;
      case Exists:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bindExists(nodeId, secondaryLookup);
        break;
      case InSub:
        bind0(plan.childOf(nodeId, 0), secondaryLookup);
        bindInSub(nodeId, secondaryLookup);
        break;
    }
  }

  private void bindAgg(int nodeId) {
    // Group By & Having can use the attribute exposed by Aggregation.
    final Values primaryLookup = valuesReg.valuesOf(nodeId);
    final Values secondaryLookup = valuesReg.valuesOf(plan.childOf(plan.childOf(nodeId, 0), 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final AggNode aggNode = (AggNode) plan.nodeAt(nodeId);

    for (Expression attr : aggNode.attrExprs()) {
      final List<Value> refs = ListSupport.map(attr.colRefs(), it -> bindRef(it, secondaryLookup));
      valuesReg.bindValueRefs(attr, refs);
    }

    for (Expression groupBy : aggNode.groupByExprs()) {
      final List<Value> refs = ListSupport.map(groupBy.colRefs(), it -> bindRef(it, lookup));
      valuesReg.bindValueRefs(groupBy, refs);
    }

    final Expression having = aggNode.havingExpr();
    if (having != null) {
      final List<Value> refs = ListSupport.map(having.colRefs(), it -> bindRef(it, lookup));
      valuesReg.bindValueRefs(having, refs);
    }
  }

  private void bindProj(int nodeId) {
    final Values lookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final ProjNode projNode = (ProjNode) plan.nodeAt(nodeId);

    for (Expression attr : projNode.attrExprs()) {
      final List<Value> refs = ListSupport.map(attr.colRefs(), it -> bindRef(it, lookup));
      valuesReg.bindValueRefs(attr, refs);
    }
  }

  private void bindJoin(int nodeId) {
    final Values lookup = valuesReg.valuesOf(nodeId);
    final JoinNode joinNode = (JoinNode) plan.nodeAt(nodeId);
    final Expression joinCond = joinNode.joinCond();
    if (joinCond == null) return;
    final List<Value> valueRefs = ListSupport.map(joinCond.colRefs(), it -> bindRef(it, lookup));
    valuesReg.bindValueRefs(joinCond, valueRefs);

    if (!SqlSupport.isEquiJoinPredicate(joinCond.template())) return;
    if ((valueRefs.size() & 1) == 1) return;

    final Values lhsValues = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lhsRefs = new ArrayList<>(valueRefs.size() >> 1);
    final List<Value> rhsRefs = new ArrayList<>(valueRefs.size() >> 1);
    for (int i = 0, bound = valueRefs.size(); i < bound; i += 2) {
      final Value key0 = valueRefs.get(i), key1 = valueRefs.get(i);
      final boolean lhs0 = lhsValues.contains(key0), lhs1 = lhsValues.contains(key1);
      if (lhs0 && !lhs1) {
        lhsRefs.add(key0);
        rhsRefs.add(key1);
      } else if (!lhs0 && lhs1) {
        lhsRefs.add(key1);
        rhsRefs.add(key0);
      } else {
        return;
      }
    }

    plan.infoCache().setJoinKeyOf(nodeId, lhsRefs, rhsRefs);
  }

  private void bindFilter(int nodeId, List<Value> secondaryLookup) {
    final Values primaryLookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final Expression predicate = ((SimpleFilterNode) plan.nodeAt(nodeId)).predicate();
    final List<Value> valueRefs = ListSupport.map(predicate.colRefs(), it -> bindRef(it, lookup));
    valuesReg.bindValueRefs(predicate, valueRefs);
  }

  private void bindExists(int nodeId, List<Value> secondaryLookup) {
    final Values primaryLookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    bind0(plan.childOf(nodeId, 1), lookup);
  }

  private void bindInSub(int nodeId, List<Value> secondaryLookup) {
    final Values primaryLookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final Expression expr = ((InSubNode) plan.nodeAt(nodeId)).expr();
    final List<Value> valueRefs = ListSupport.map(expr.colRefs(), it -> bindRef(it, lookup));
    valuesReg.bindValueRefs(expr, valueRefs);
    bind0(plan.childOf(nodeId, 1), lookup);
    // TODO: setPlain
  }

  private void bindSort(int nodeId) {
    // Order By can use the attributes exposed in table-source
    // e.g., Select t.x From t Order By t.y
    // So we have to lookup in deeper descendant.
    final int child = plan.childOf(nodeId, 0);
    final int grandChild = plan.childOf(child, 0);
    final PlanKind childKind = plan.kindOf(child);

    final Values secondaryLookup = valuesReg.valuesOf(child);
    final List<Value> primaryLookup;
    if (childKind == PlanKind.Proj) {
      primaryLookup = valuesReg.valuesOf(grandChild);
    } else if (childKind == PlanKind.Agg) {
      primaryLookup = valuesReg.valuesOf(plan.childOf(grandChild, 0));
    } else if (childKind == PlanKind.SetOp) {
      primaryLookup = Collections.emptyList();
    } else {
      throw failed("unexpected plan: " + plan);
    }

    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);

    for (Expression sortSpec : ((SortNode) plan.nodeAt(nodeId)).sortSpec()) {
      final List<Value> refs = ListSupport.map(sortSpec.colRefs(), it -> bindRef(it, lookup));
      valuesReg.bindValueRefs(sortSpec, refs);
    }
  }

  private Value bindRef(SqlNode colRef, List<Value> lookup) {
    final SqlNode colName = colRef.$(ColRef_ColName);
    final String qualification = colName.$(ColName_Table);
    final String name = colName.$(ColName_Col);

    for (Value value : lookup) {
      if ((qualification == null || qualification.equals(value.qualification()))
          && name.equals(value.name())) {
        return value;
      }
    }

    return null;
  }

  private PlanException failed(String reason) {
    return new PlanException("failed to bind ref: [" + reason + "] " + plan);
  }
}
