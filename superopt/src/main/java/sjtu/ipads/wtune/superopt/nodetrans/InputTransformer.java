package sjtu.ipads.wtune.superopt.nodetrans;

import com.microsoft.z3.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeNode.SPJNode;
import sjtu.ipads.wtune.spes.AlgeNode.TableNode;
import sjtu.ipads.wtune.sqlparser.plan1.InputNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputTransformer extends BaseTransformer {

  @Override
  public AlgeNode transform() {
    InputNode input = ((InputNode) planNode);

    List<RelDataType> columnTypes = new ArrayList<>();
    input.table().columns().forEach(col -> columnTypes.add(defaultIntType()));

    TableNode tableNode = new TableNode(input.table().name(), columnTypes, z3Context);
    return wrapBySPJ(tableNode, z3Context);
  }

  private SPJNode wrapBySPJ(TableNode tableNode, Context z3Context) {
    Set<RexNode> emptyCondition = new HashSet<>();
    List<AlgeNode> inputs = new ArrayList<AlgeNode>();
    inputs.add(tableNode);
    return (new SPJNode(tableNode.getOutputExpr(), emptyCondition, inputs, z3Context));
  }
}
