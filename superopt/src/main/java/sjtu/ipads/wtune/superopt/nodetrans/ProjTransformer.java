package sjtu.ipads.wtune.superopt.nodetrans;

import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeNode.SPJNode;
import sjtu.ipads.wtune.spes.AlgeNode.UnionNode;
import sjtu.ipads.wtune.spes.RexNodeHelper.RexNodeHelper;
import sjtu.ipads.wtune.sqlparser.plan.Expression;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan.Value;
import sjtu.ipads.wtune.sqlparser.plan.Values;

import java.util.ArrayList;
import java.util.List;

public class ProjTransformer extends BaseTransformer {
  private RexNode Expression2RexNode(Expression expr) {
    Values valuesOfInput = planCtx.valuesOf(planNode.child(planCtx, 0));

    // Get a value's index just in projected values
    // Since expr's valueRefs may not equal to values on this plan node
    Values values = planCtx.valuesReg().valueRefsOf(expr);
    assert !values.isEmpty();

    List<RexInputRef> rexRefs = new ArrayList<>(values.size());
    for (Value val : values) {
      int idx = valuesOfInput.indexOf(val);
      if (idx < 0) return null;

      rexRefs.add(new RexInputRef(idx, defaultIntType()));
    }

    return rexRefs.get(0);
  }

  @Override
  public AlgeNode transform() {
    ProjNode proj = ((ProjNode) planNode);
    AlgeNode childNode = transformNode(proj.child(planCtx, 0), planCtx, z3Context);

    List<RexNode> columns = new ArrayList<>();
    for (Expression projExpr : proj.attrExprs()) {
      RexNode rexRef = Expression2RexNode(projExpr);
      if (rexRef == null) return null;
      columns.add(rexRef);
    }

    if (childNode instanceof UnionNode) {
      updateUnion((UnionNode) childNode, columns);
    }
    if (childNode instanceof SPJNode) {
      updateSPJ(childNode, columns);
    } else {
      // System.out.println("error in project parser:" + childNode.toString());
    }

    if (proj.deduplicated()) return AggTranformer.distinctToAgg(childNode);
    return childNode;
  }

  private void updateSPJ(AlgeNode spjNode, List<RexNode> columns) {
    updateOutputExprs(spjNode, columns);
  }

  private void updateUnion(UnionNode unionNode, List<RexNode> columns) {
    for (AlgeNode input : unionNode.getInputs()) {
      updateOutputExprs(input, columns);
    }
  }

  private void updateOutputExprs(AlgeNode inputNode, List<RexNode> columns) {
    List<RexNode> inputExprs = inputNode.getOutputExpr();
    List<RexNode> newOutputExpr = new ArrayList<>();
    for (RexNode expr : columns) {
      newOutputExpr.add(RexNodeHelper.substitute(expr, inputExprs));
    }
    inputNode.setOutputExpr(newOutputExpr);
  }
}
