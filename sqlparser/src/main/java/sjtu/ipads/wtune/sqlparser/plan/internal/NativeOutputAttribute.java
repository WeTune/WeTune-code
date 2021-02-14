package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;

public class NativeOutputAttribute extends OutputAttributeBase {
  private final Column column;

  private NativeOutputAttribute(PlanNode owner, String qualification, String name, Column column) {
    super(owner, qualification, name);
    this.column = column;
  }

  public static List<OutputAttribute> build(PlanNode node, Relation rel) {
    final List<Attribute> attributes = rel.attributes();
    final List<OutputAttribute> outAttrs = new ArrayList<>(attributes.size());
    for (Attribute attr : attributes)
      outAttrs.add(new NativeOutputAttribute(node, rel.alias(), attr.name(), attr.column(true)));
    return outAttrs;
  }

  @Override
  public ASTNode node() {
    return null;
  }

  @Override
  public OutputAttribute reference() {
    return null;
  }

  @Override
  public Column column() {
    return column;
  }
}
