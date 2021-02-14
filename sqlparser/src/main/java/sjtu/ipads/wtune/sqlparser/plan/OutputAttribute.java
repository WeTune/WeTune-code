package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.internal.DerivedOutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.internal.NativeOutputAttribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;

public interface OutputAttribute {
  String qualification();

  String name();

  PlanNode owner();

  OutputAttribute reference();

  ASTNode node();

  Column column();

  default ASTNode toColumnRef() {
    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, qualification());
    colName.set(COLUMN_NAME_COLUMN, name());

    final ASTNode ref = ASTNode.expr(ExprKind.COLUMN_REF);
    ref.set(COLUMN_REF_COLUMN, colName);

    return ref;
  }

  static List<OutputAttribute> fromInput(PlanNode node, Relation rel) {
    return NativeOutputAttribute.build(node, rel);
  }

  static List<OutputAttribute> fromProj(PlanNode node, Relation rel) {
    return DerivedOutputAttribute.build(node, rel);
  }
}
