package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.Arrays;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.OutputAttribute.fromProj;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRef;

public class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private final List<OutputAttribute> projection;
  private final ASTNode[] selectItems;
  private final List<ASTNode> columnRefs;

  private ProjNodeImpl(Relation relation) {
    this.projection = fromProj(this, relation);
    this.selectItems = arrayMap(Attribute::toSelectItem, ASTNode.class, relation.attributes());
    this.columnRefs = collectColumnRef(selectItems);
  }

  public static ProjNodeImpl build(Relation projection) {
    return new ProjNodeImpl(projection);
  }

  @Override
  public List<ASTNode> selectItems() {
    final ASTNode[] copies = arrayMap(ASTNode::copy, ASTNode.class, selectItems);

    final List<ASTNode> columnRefs = collectColumnRef(copies);
    final List<OutputAttribute> usedAttrs = usedAttributes0(columnRefs);

    for (int i = 0, bound = columnRefs.size(); i < bound; i++) {
      final OutputAttribute usedAttr = usedAttrs.get(i);
      if (usedAttr != null) columnRefs.get(i).update(usedAttr.toColumnRef());
    }

    return Arrays.asList(copies);
  }

  @Override
  public List<OutputAttribute> outputAttributes() {
    return projection;
  }

  @Override
  public List<OutputAttribute> usedAttributes() {
    return usedAttributes0(columnRefs);
  }

  private List<OutputAttribute> usedAttributes0(List<ASTNode> columnRefs) {
    return listMap(predecessors()[0]::outputAttribute, columnRefs);
  }
}
