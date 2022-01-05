package sjtu.ipads.wtune.sql.plan;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

import java.util.LinkedList;
import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;

class DependentRefInspector {
  private final PlanContext plan;

  private List<Value> outerValues;
  private Lazy<List<Value>> depValRefs;
  private Lazy<List<SqlNode>> depColRefs;

  DependentRefInspector(PlanContext plan) {
    this.plan = plan;
  }

  static Pair<List<Value>, List<SqlNode>> inspectDepRefs(PlanContext plan, int subqueryRoot) {
    final DependentRefInspector inspector = new DependentRefInspector(plan);
    inspector.inspect(subqueryRoot);
    return Pair.of(inspector.dependentValueRefs(), inspector.dependentColRefs());
  }

  void inspect(int subqueryRoot) {
    final int parent = plan.parentOf(subqueryRoot);

    assert parent != NO_SUCH_NODE;
    assert plan.kindOf(parent).isSubqueryFilter();
    assert plan.childOf(parent, 1) == subqueryRoot;

    outerValues = plan.valuesReg().valuesOf(plan.childOf(parent, 0));
    depValRefs = Lazy.mk(LinkedList::new);
    depColRefs = Lazy.mk(LinkedList::new);
    inspect0(subqueryRoot);
  }

  public List<Value> dependentValueRefs() {
    return depValRefs.get();
  }

  public List<SqlNode> dependentColRefs() {
    return depColRefs.get();
  }

  private void inspect0(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    switch (kind) {
      case Input:
      case Exists:
      case SetOp:
      case Sort:
      case Limit:
      case Agg:
        break;

      case Proj:
        for (Expression expr : ((ProjNode) plan.nodeAt(nodeId)).attrExprs()) inspectExpr(expr);
        break;
      case Filter:
        inspectExpr(((SimpleFilterNode) plan.nodeAt(nodeId)).predicate());
        break;
      case InSub:
        inspectExpr(((InSubNode) plan.nodeAt(nodeId)).expr());
        break;
      case Join:
        inspectExpr(((JoinNode) plan.nodeAt(nodeId)).joinCond());
        break;
    }

    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) {
      inspect0(plan.childOf(nodeId, i));
    }
  }

  private void inspectExpr(Expression expr) {
    if (expr == null) return;
    final Values refs = plan.valuesReg().valueRefsOf(expr);
    final List<SqlNode> colRefs = expr.colRefs();
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final Value ref = refs.get(i);
      if (outerValues.contains(ref)) {
        depValRefs.get().add(ref);
        depColRefs.get().add(colRefs.get(i));
      }
    }
  }
}
