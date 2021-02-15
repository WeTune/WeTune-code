package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.plan.OutputAttribute.fromProj;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRefs;

public class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private final List<OutputAttribute> projection;
  private List<OutputAttribute> usedAttributes;

  private ProjNodeImpl(Relation relation) {
    this.projection = fromProj(this, relation);
    this.usedAttributes = null;
  }

  private ProjNodeImpl(List<OutputAttribute> projs) {
    this.projection = projs;
  }

  public static ProjNodeImpl build(Relation projection) {
    return new ProjNodeImpl(projection);
  }

  public static ProjNodeImpl build(List<OutputAttribute> projs) {
    return new ProjNodeImpl(projs);
  }

  @Override
  public List<ASTNode> selectItems() {
    final List<ASTNode> items = listMap(OutputAttribute::toSelectItem, projection);
    final List<ASTNode> columnRefs = collectColumnRefs(items);

    if (columnRefs.size() != usedAttributes.size()) System.err.println("Mismatched attributes");

    for (int i = 0, bound = columnRefs.size(); i < bound; i++) {
      final OutputAttribute usedAttr = usedAttributes.get(i);
      if (usedAttr != null) columnRefs.get(i).update(usedAttr.toColumnRef());
    }

    return items;
  }

  @Override
  public List<OutputAttribute> outputAttributes() {
    return projection;
  }

  @Override
  public List<OutputAttribute> usedAttributes() {
    return usedAttributes;
  }

  @Override
  public void resolveUsedAttributes() {
    final PlanNode input = predecessors()[0];

    for (OutputAttribute proj : projection) {
      final OutputAttribute resolved = proj.reference(false);
      final String[] refName = proj.referenceName();
      if (resolved == proj && refName != null) {
        final OutputAttribute ref = input.resolveAttribute(refName[0], refName[1]);
        proj.setReference(ref);
      }

      final List<OutputAttribute> used = proj.used();
      if (used == null)
        if (WILDCARD.isInstance(proj.expr())) proj.setUsed(singletonList(proj.reference(false)));
        else proj.setUsed(resolveUsedAttributes0(collectColumnRefs(proj.expr()), input));
      else proj.setUsed(resolveUsedAttributes1(proj.used(), input));
    }

    usedAttributes =
        projection.stream().flatMap(it -> it.used().stream()).collect(Collectors.toList());
  }

  @Override
  protected PlanNode copy0() {
    return new ProjNodeImpl(projection);
  }
}
