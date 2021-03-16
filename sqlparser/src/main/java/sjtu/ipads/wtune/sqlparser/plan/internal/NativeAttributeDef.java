package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.List;

import static java.util.Collections.singletonList;
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
  public boolean isIdentity() {
    return true;
  }

  @Override
  public List<AttributeDef> references() {
    return singletonList(this);
  }

  @Override
  public AttributeDef upstream() {
    return this;
  }

  @Override
  public AttributeDef source() {
    return this;
  }

  @Override
  public AttributeDef nativeSource() {
    return this;
  }

  @Override
  public Column referredColumn() {
    return column;
  }

  @Override
  public boolean referencesTo(String qualification, String alias) {
    return (qualification == null || qualification.equals(this.qualification()))
        && alias.equals(this.name());
  }

  @Override
  public boolean referencesTo(int id) {
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
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof AttributeDef)) return false;

    if (o instanceof NativeAttributeDef) return ((NativeAttributeDef) o).id() == this.id();
    else return o.equals(this);
  }

  @Override
  public String toString() {
    return column + " AS " + qualification() + "." + name();
  }

  @Override
  public AttributeDef copy() {
    return new NativeAttributeDef(this.id(), qualification(), referredColumn());
  }
}
