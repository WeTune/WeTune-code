package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;

public abstract class AttributeDefBase implements AttributeDef {
  private final int id;
  private final String qualification;
  private final String name;

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
  public AttributeDef copy() {
    return copyWithQualification(qualification());
  }

  @Override
  public ASTNode makeColumnRef() {
    if (name() == null) throw new IllegalStateException("anonymous attribute cannot be referenced");

    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, qualification());
    colName.set(COLUMN_NAME_COLUMN, name());

    final ASTNode ref = ASTNode.expr(COLUMN_REF);
    ref.set(COLUMN_REF_COLUMN, colName);

    return ref;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AttributeDef that)) return false;
    final AttributeDef thisSrc = this.source(), thatSrc = that.source();
    return thisSrc.id() == thatSrc.id();
  }

  @Override
  public int hashCode() {
    return source().id();
  }
}
