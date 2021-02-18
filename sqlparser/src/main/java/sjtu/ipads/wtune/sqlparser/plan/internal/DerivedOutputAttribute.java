package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.relational.internal.DerivedAttribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;

public class DerivedOutputAttribute extends OutputAttributeBase {
  private final ASTNode expr;
  private final String[] referenceName;
  private OutputAttribute reference;
  private List<OutputAttribute> used;

  public DerivedOutputAttribute(
      PlanNode source, String qualification, String name, ASTNode expr, String[] referenceName) {
    super(source, qualification, name);
    this.expr = expr;
    this.referenceName = referenceName;
  }

  public static List<OutputAttribute> build(PlanNode source, Relation relation) {
    final List<Attribute> attrs = relation.attributes();
    final List<OutputAttribute> outAttrs = new ArrayList<>(attrs.size());
    for (Attribute attr : attrs) {
      assert attr instanceof DerivedAttribute;

      final ASTNode expr = attr.selectItem().get(SELECT_ITEM_EXPR);
      final Attribute ref = attr.reference(false);
      final String[] referenceName =
          ref != null ? new String[] {ref.owner().alias(), ref.name()} : null;

      outAttrs.add(
          new DerivedOutputAttribute(source, relation.alias(), attr.name(), expr, referenceName));
    }
    return outAttrs;
  }

  @Override
  public String[] referenceName() {
    return referenceName;
  }

  @Override
  public ASTNode expr() {
    return expr;
  }

  @Override
  public Column column(boolean recursive) {
    final OutputAttribute ref = reference(true);
    return ref == null ? null : ref.column(true);
  }

  @Override
  public OutputAttribute reference(boolean recursive) {
    if (!recursive || reference == null) return reference;
    return reference.reference(true);
  }

  @Override
  public List<OutputAttribute> used() {
    return used;
  }

  @Override
  public void setReference(OutputAttribute reference) {
    this.reference = reference;
  }

  @Override
  public void setUsed(List<OutputAttribute> used) {
    this.used = used;
  }

  @Override
  public boolean refEquals(OutputAttribute other) {
    if (other instanceof NativeOutputAttribute) return reference(true) == other;
    else if (other instanceof DerivedOutputAttribute)
      return reference(true) == other.reference(true);
    else return false;
  }
}
