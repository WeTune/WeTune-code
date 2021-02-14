package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

public class DerivedOutputAttribute extends OutputAttributeBase {
  private final ASTNode node;

  public DerivedOutputAttribute(PlanNode owner, String qualification, String name, ASTNode node) {
    super(owner, qualification, name);
    this.node = node;
  }

  public static List<OutputAttribute> build(PlanNode node, Relation relation) {
    final List<Attribute> attrs = relation.attributes();
    final List<OutputAttribute> outAttrs = new ArrayList<>(attrs.size());
    for (Attribute attr : attrs)
      outAttrs.add(new DerivedOutputAttribute(node, relation.alias(), attr.name(), attr.node()));
    return outAttrs;
  }

  @Override
  public OutputAttribute reference() {
    final ASTNode expr = node.get(SELECT_ITEM_EXPR);
    if (!COLUMN_REF.isInstance(expr)) return null;

    final ASTNode columnName = expr.get(COLUMN_REF_COLUMN);
    final String qualification = columnName.get(COLUMN_NAME_TABLE);
    final String name = columnName.get(COLUMN_NAME_COLUMN);

    return owner().predecessors()[0].outputAttribute(qualification, name);
  }

  @Override
  public ASTNode node() {
    return node;
  }

  @Override
  public Column column() {
    return null;
  }
}
