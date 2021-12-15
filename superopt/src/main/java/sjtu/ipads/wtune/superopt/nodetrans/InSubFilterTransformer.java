package sjtu.ipads.wtune.superopt.nodetrans;

import com.microsoft.z3.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeNode.SPJNode;
import sjtu.ipads.wtune.spes.AlgeNode.UnionNode;
import sjtu.ipads.wtune.spes.RexNodeHelper.RexNodeHelper;
import sjtu.ipads.wtune.sqlparser.plan1.InSubNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;
import sjtu.ipads.wtune.sqlparser.plan1.Values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InSubFilterTransformer extends BaseTransformer {
  private static RexNode InSubFilter2JoinCondition(
      List<RexNode> leftOutput, List<RexNode> rightOutput) {
    if (leftOutput.size() != rightOutput.size()) return null;

    List<RexNode> eqConditions = new ArrayList<>();
    for (int i = 0, bound = leftOutput.size(); i < bound; i++) {
      eqConditions.add(
          rexBuilder.makeCall(
              SqlStdOperatorTable.EQUALS, List.of(leftOutput.get(i), rightOutput.get(i))));
    }

    if (eqConditions.isEmpty()) return null;
    // a1=b1 AND a2=b2 AND a3=b3 ...
    return eqConditions.size() < 2
        ? eqConditions.get(0)
        : (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.AND, eqConditions);
  }

  private List<RexNode> leftExpr(InSubNode inSubFilter) {
    Values valuesOfNode = planCtx.valuesOf(planNode);
    Values filterValues = planCtx.valuesReg().valueRefsOf(inSubFilter.expr());

    List<RexNode> filterRexNodes = new ArrayList<>();
    for (Value filterVal : filterValues) {
      int idx = valuesOfNode.indexOf(filterVal);
      filterRexNodes.add(new RexInputRef(idx, defaultIntType()));
    }

    return filterRexNodes;
  }

  private List<RexNode> rightExpr(List<RexNode> rightOutput, int offset) {
    List<RexNode> rightOutputWithOffset = new ArrayList<>(rightOutput.size());
    for (RexNode rexNode : rightOutput) {
      int idxPlusOffset = ((RexInputRef) rexNode).getIndex() + offset;
      rightOutputWithOffset.add(new RexInputRef(idxPlusOffset, rexNode.getType()));
    }
    return rightOutputWithOffset;
  }

  @Override
  public AlgeNode transform() {
    InSubNode inSubFilter = ((InSubNode) planNode);
    AlgeNode leftInput = transformNode(inSubFilter.child(planCtx, 0), planCtx, z3Context);
    AlgeNode rightSubQuery = transformNode(inSubFilter.child(planCtx, 1), planCtx, z3Context);

    List<RexNode> leftOutput = leftExpr(inSubFilter);
    List<RexNode> rightOutput =
        rightExpr(rightSubQuery.getOutputExpr(), leftInput.getOutputExpr().size());
    RexNode joinCondition = InSubFilter2JoinCondition(leftOutput, rightOutput);

    if (joinCondition == null) return null;

    List<AlgeNode> result = innerJoinAll(leftInput, rightSubQuery, z3Context, joinCondition);
    // InSub Filter should be like InnerJoin
    return constructNode(result, z3Context);
  }

  private AlgeNode constructNode(List<AlgeNode> result, Context z3Context) {
    if (result.size() == 1) {
      return result.get(0);
    } else {
      List<RelDataType> inputTypes = new ArrayList<>();
      for (RexNode column : result.get(0).getOutputExpr()) {
        inputTypes.add(column.getType());
      }
      return new UnionNode(result, z3Context, inputTypes);
    }
  }

  private List<AlgeNode> innerJoinAll(
      AlgeNode leftNode, AlgeNode rightNode, Context z3Context, RexNode joinCondition) {
    List<AlgeNode> leftInputs = new ArrayList<>();
    if (leftNode instanceof UnionNode) {
      leftInputs.addAll(leftNode.getInputs());
    } else {
      leftInputs.add(leftNode);
    }
    List<AlgeNode> rightInputs = new ArrayList<>();
    if (rightNode instanceof UnionNode) {
      rightInputs.addAll(rightNode.getInputs());
    } else {
      rightInputs.add(rightNode);
    }
    List<AlgeNode> result = new ArrayList<>();
    for (AlgeNode left : leftInputs) {
      for (AlgeNode right : rightInputs) {
        result.add(innerJoin(left, right, z3Context, joinCondition));
      }
    }
    return result;
  }

  private SPJNode innerJoin(
      AlgeNode leftNode, AlgeNode rightNode, Context z3Context, RexNode joinCondition) {

    // getInput tables
    List<AlgeNode> inputs = new ArrayList<>();
    addInputs(leftNode, inputs);
    int offSize = 0;
    for (AlgeNode leftInput : inputs) {
      offSize = offSize + leftInput.getOutputExpr().size();
    }
    addInputs(rightNode, inputs);

    // build new output expr;
    List<RexNode> newOutputExpr = new ArrayList<>(leftNode.getOutputExpr());

    List<RexNode> rightOutputExpr = rightNode.getOutputExpr();
    for (RexNode rexNode : rightOutputExpr) {
      newOutputExpr.add(RexNodeHelper.addOffSize(rexNode, offSize));
    }

    // build new condition;
    Set<RexNode> newCondition = new HashSet<>(leftNode.getConditions());

    Set<RexNode> rightConditions = rightNode.getConditions();
    for (RexNode rightCondition : rightConditions) {
      newCondition.add(RexNodeHelper.addOffSize(rightCondition, offSize));
    }
    RexNode newJoinCondition = RexNodeHelper.substitute(joinCondition, newOutputExpr);
    newCondition.add(newJoinCondition);

    return new SPJNode(newOutputExpr, newCondition, inputs, z3Context);
  }

  private void addInputs(AlgeNode child, List<AlgeNode> inputs) {
    if (child instanceof SPJNode) {
      inputs.addAll(child.getInputs());
    } else {
      inputs.add(child);
    }
  }
}
