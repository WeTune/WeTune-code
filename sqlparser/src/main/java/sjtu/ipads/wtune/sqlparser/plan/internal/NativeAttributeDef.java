package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;

public class NativeAttributeDef extends AttributeDefBase {
  private final Column column;

  private NativeAttributeDef(int id, String qualification, Column column) {
    super(id, qualification, column.name());
    this.column = column;
  }

  public static AttributeDef fromColumn(int id, String tableAlias, Column c) {
    return new NativeAttributeDef(id, tableAlias, c);
  }

  @Override
  public Column referredColumn() {
    return column;
  }

  @Override
  public int[] references() {
    return new int[] {this.id()};
  }

  @Override
  public AttributeDef upstream() {
    return this;
  }

  @Override
  public AttributeDef nativeUpstream() {
    return this;
  }

  @Override
  public boolean isIdentity() {
    return true;
  }

  @Override
  public boolean isReferencedBy(String qualification, String alias) {
    return (qualification == null || qualification.equals(this.qualification()))
        && alias.equals(this.name());
  }

  @Override
  public boolean isReferencedBy(int id) {
    return this.id() == id;
  }

  @Override
  public ASTNode toSelectItem() {
    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, qualification());
    colName.set(COLUMN_NAME_COLUMN, column.name());

    final ASTNode colRef = ASTNode.expr(ExprKind.COLUMN_REF);
    colRef.set(COLUMN_REF_COLUMN, colName);

    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, colRef);
    item.set(SELECT_ITEM_ALIAS, this.name());

    return item;
  }

  @Override
  public String toString() {
    return column + " AS " + qualification() + "." + name();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof NativeAttributeDef) return ((NativeAttributeDef) o).id() == this.id();
    else if (o instanceof DerivedAttributeDef) return o.equals(this);
    else return false;
  }

  @Override
  public int hashCode() {
    throw new IllegalArgumentException();
  }

  @Override
  public AttributeDef copy() {
    return new NativeAttributeDef(this.id(), qualification(), referredColumn());
  }
}
