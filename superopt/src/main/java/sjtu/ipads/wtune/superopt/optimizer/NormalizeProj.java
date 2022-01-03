package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.plan.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.tree.TreeSupport.indexOfChild;
import static sjtu.ipads.wtune.common.utils.IterableSupport.zip;
import static sjtu.ipads.wtune.sql.plan.PlanKind.*;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.mkColRefExpr;

class NormalizeProj {
  private final PlanContext plan;

  NormalizeProj(PlanContext plan) {
    this.plan = plan;
  }

  int normalizeTree(int rootId) {
    final PlanKind kind = plan.kindOf(rootId);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i)
      normalizeTree(plan.childOf(rootId, i));

    if (kind == Proj && shouldReduceProj(rootId)) return reduceProj(rootId);
    if (shouldInsertProjBefore(rootId)) return insertProjBefore(rootId);
    return rootId;
  }

  int insertProjBefore(int position) {
    final int parent = plan.parentOf(position);
    assert parent != NO_SUCH_NODE;

    final ValuesRegistry valuesReg = plan.valuesReg();
    final List<Value> inputs = valuesReg.valuesOf(position);

    final List<String> outputNames = new ArrayList<>(inputs.size());
    final List<Expression> outputExprs = new ArrayList<>(inputs.size());
    for (final Value value : inputs) {
      outputNames.add(value.name());
      outputExprs.add(mkColRefExpr(value));
    }

    final ProjNode proj = ProjNode.mk(false, outputNames, outputExprs);
    final int projNode = plan.bindNode(proj);
    final int childIdx = indexOfChild(plan, position);
    plan.setChild(parent, childIdx, projNode);
    plan.setChild(projNode, 0, position);

    final Values outputs = valuesReg.valuesOf(projNode);
    final Set<Expression> excludedExprs = gatherExcludedExprs(projNode, new HashSet<>());

    zip(outputExprs, inputs, (expr, ref) -> valuesReg.bindValueRefs(expr, newArrayList(ref)));
    zip(inputs, outputs, (oldRef, newRef) -> valuesReg.displaceRef(oldRef, newRef, excludedExprs));
    PlanSupport.disambiguateQualification(plan);

    return projNode;
  }

  int reduceProj(int proj) {
    assert plan.kindOf(proj) == Proj;

    final int parent = plan.parentOf(proj);
    assert parent != NO_SUCH_NODE;

    final int childIdx = indexOfChild(plan, proj);
    final int replacement = plan.childOf(proj, 0);
    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values oldRefs = valuesReg.valuesOf(proj);
    final Values newRefs = valuesReg.valuesOf(replacement);
    assert oldRefs.size() == newRefs.size();

    zip(oldRefs, newRefs, (oldRef, newRef) -> valuesReg.displaceRef(oldRef, newRef, emptySet()));

    plan.detachNode(replacement);
    plan.setChild(parent, childIdx, replacement);

    return replacement;
  }

  boolean shouldInsertProjBefore(int node) {
    final int parent = plan.parentOf(node);
    final PlanKind nodeKind = plan.kindOf(node);
    if (nodeKind == Filter) return parent == NO_SUCH_NODE || plan.kindOf(parent) == Join;
    if (nodeKind == Join) return parent == NO_SUCH_NODE;
    return false;
  }

  boolean shouldReduceProj(int node) {
    final PlanKind nodeKind = plan.kindOf(node);
    if (nodeKind != Proj) return false;

    final int parent = plan.parentOf(node);
    final int child = plan.childOf(node, 0);

    if (parent == NO_SUCH_NODE) return false;
    if (plan.kindOf(child).isFilter() && !plan.kindOf(parent).isFilter()) return false;

    final Values inputs = plan.valuesReg().valuesOf(child);
    final Values outputs = plan.valuesReg().valuesOf(node);
    if (inputs.size() != outputs.size()) return false;

    for (int i = 0, bound = inputs.size(); i < bound; i++) {
      if (PlanSupport.deRef(plan, outputs.get(i)) != inputs.get(i)) return false;
    }

    return true;
  }

  private Set<Expression> gatherExcludedExprs(int nodeId, Set<Expression> exprs) {
    final PlanKind kind = plan.kindOf(nodeId);
    switch (kind) {
      case Input:
      case Exists:
      case SetOp:
      case Limit:
        break;
      case Filter:
        exprs.add(((SimpleFilterNode) plan.nodeAt(nodeId)).predicate());
        break;
      case InSub:
        exprs.add(((InSubNode) plan.nodeAt(nodeId)).expr());
        break;
      case Proj:
        exprs.addAll(((ProjNode) plan.nodeAt(nodeId)).attrExprs());
        break;
      case Agg:
        {
          final AggNode agg = (AggNode) plan.nodeAt(nodeId);
          exprs.addAll(agg.attrExprs());
          exprs.addAll(agg.groupByExprs());
          if (agg.havingExpr() != null) exprs.add(agg.havingExpr());
          break;
        }
      case Sort:
        exprs.addAll(((SortNode) plan.nodeAt(nodeId)).sortSpec());
        break;
      case Join:
        exprs.add(((JoinNode) plan.nodeAt(nodeId)).joinCond());
        break;
    }

    for (int i = 0, bound = kind.numChildren(); i < bound; ++i)
      gatherExcludedExprs(plan.childOf(nodeId, i), exprs);
    return exprs;
  }
}
