package sjtu.ipads.wtune.superopt.nodetrans;

import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeNode.SPJNode;
import sjtu.ipads.wtune.spes.AlgeNode.UnionNode;
import sjtu.ipads.wtune.spes.RexNodeHelper.RexNodeHelper;
import sjtu.ipads.wtune.sqlparser.plan.Expression;
import sjtu.ipads.wtune.sqlparser.plan.SimpleFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.Value;
import sjtu.ipads.wtune.sqlparser.plan.Values;

import java.util.ArrayList;
import java.util.List;

public class SimpleFilterTransformer extends BaseTransformer {
  private RexNode Expression2FilterCondition(Expression filterCond) {
    // Likewise, value in `joinCondValues` is in `valuesOfNode`
    Values valuesOfNode = planCtx.valuesOf(planNode);
    Values predValues = planCtx.valuesReg().valueRefsOf(filterCond);

    // A little dirty code
    Comparable<Integer> predId = Integer.parseInt(filterCond.toString().substring(1, 2));
    RexLiteral literal = (RexLiteral) rexBuilder.makeLiteral(predId, defaultIntType(), false);

    List<RexNode> predList = new ArrayList<>();
    for (Value predVal : predValues) {
      int idx = valuesOfNode.indexOf(predVal);
      // $i
      RexInputRef inputRef = new RexInputRef(idx, defaultIntType());
      // =($i, n)
      RexCall rexCall =
          (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, List.of(inputRef, literal));
      predList.add(rexCall);
    }
    if (predList.isEmpty()) return null;
    // p1 AND p2 AND p3 ...
    return predList.size() < 2
        ? predList.get(0)
        : (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.AND, predList);
  }

  @Override
  public AlgeNode transform() {
    SimpleFilterNode filter = ((SimpleFilterNode) planNode);
    RexNode filterCondition = Expression2FilterCondition(filter.predicate());

    if (filterCondition == null) return null;

    AlgeNode inputNode = transformNode(filter.child(planCtx, 0), planCtx, z3Context);
    if (inputNode instanceof UnionNode) {
      return distributeCondition((UnionNode) inputNode, filterCondition);
    }
    if (inputNode instanceof SPJNode) {
      return SPJNode(inputNode, filterCondition);
    } else {
      // System.out.println("error in filter parser" + inputNode.toString());
      return inputNode;
    }
  }

  private AlgeNode SPJNode(AlgeNode spjNode, RexNode condition) {
    RexNode newCondition = RexNodeHelper.substitute(condition, spjNode.getOutputExpr());
    spjNode.addConditions(conjunctiveForm(newCondition));
    return spjNode;
  }

  private UnionNode distributeCondition(UnionNode unionNode, RexNode condition) {
    for (AlgeNode input : unionNode.getInputs()) {
      RexNode newCondition = RexNodeHelper.substitute(condition, input.getOutputExpr());
      input.addConditions(conjunctiveForm(newCondition));
    }
    return unionNode;
  }

  public static List<RexNode> conjunctiveForm(RexNode condition) {
    if (condition instanceof RexCall) {
      RexCall rexCall = (RexCall) condition;
      if (rexCall.isA(SqlKind.AND)) {
        return rexCall.getOperands();
      }
    }
    List<RexNode> result = new ArrayList<>();
    result.add(condition);
    return result;
  }
}
