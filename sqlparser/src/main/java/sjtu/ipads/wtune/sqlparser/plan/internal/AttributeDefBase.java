package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public abstract class AttributeDefBase implements AttributeDef {
  private final int id;
  private String qualification;
  private final String name;

  private PlanNode definer;

  public AttributeDefBase(int id, String qualification, String name) {
    this.id = id == -1 ? 0 : id;
    this.qualification = simpleName(qualification);
    this.name = simpleName(name);
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public PlanNode definer() {
    return definer;
  }

  @Override
  public void setDefiner(PlanNode definer) {
    this.definer = definer;
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

  @Override
  public ASTNode toColumnRef() {
    if (name() == null) throw new IllegalStateException("anonymous attribute cannot be referenced");

    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, qualification());
    colName.set(COLUMN_NAME_COLUMN, name());

    final ASTNode ref = ASTNode.expr(COLUMN_REF);
    ref.set(COLUMN_REF_COLUMN, colName);

    return ref;
  }

  @Override
  public int hashCode() {
    // this class should never be used as Map key or put into set
    throw new UnsupportedOperationException();
  }
}
