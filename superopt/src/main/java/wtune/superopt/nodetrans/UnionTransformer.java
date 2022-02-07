package wtune.superopt.nodetrans;

import org.apache.calcite.rel.type.RelDataType;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeNode.UnionNode;
import wtune.sql.plan.*;

import java.util.ArrayList;
import java.util.List;


public class UnionTransformer extends BaseTransformer{
  @Override
  public AlgeNode transform() {
    SetOpNode union = ((SetOpNode) planNode);
    List<AlgeNode> inputs = new ArrayList<>();
    for (PlanNode child : union.children(planCtx)) {
      AlgeNode childNode = transformNode(child, planCtx, z3Context);
      inputs.addAll(normalizeNodes(childNode));
    }
    List<RelDataType> inputTypes = new ArrayList<>();
    for(int i = 0, bound = colNumOfInput(union, planCtx); i < bound; i++) {
      inputTypes.add(defaultIntType());
    }
    UnionNode unionNode = new UnionNode(inputs, z3Context, inputTypes);

    if (union.deduplicated()) return AggTranformer.distinctToAgg(unionNode);
    else return unionNode;
  }

  private List<AlgeNode> normalizeNodes(AlgeNode input) {
    List<AlgeNode> result = new ArrayList<>();
    if (input instanceof UnionNode) result.addAll(input.getInputs());
    else result.add(input);

    return result;
  }

  private int colNumOfInput(PlanNode planNode, PlanContext planCtx) {
    return switch (planNode.kind()) {
      case Input -> ((InputNode) planNode).table().columns().size();
      case Proj -> ((ProjNode) planNode).attrExprs().size();
      case Filter, InSub, Exists -> colNumOfInput(planNode.child(planCtx, 0), planCtx);
      case Join -> colNumOfInput(planNode.child(planCtx, 0), planCtx)
          + colNumOfInput(planNode.child(planCtx, 1), planCtx);
      case Agg -> ((AggNode) planNode).attrExprs().size();
      case SetOp -> colNumOfInput(planNode.child(planCtx, 0), planCtx);
      default -> throw new IllegalStateException("Unsupported planNode type: " + planNode.kind());
    };
  }
}
