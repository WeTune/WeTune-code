package sjtu.ipads.wtune.sqlparser.plan.internal;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.schema.Column;

public class NativeAttributeDef extends AttributeDefBase {
  private final Column column;

  private NativeAttributeDef(int id, String qualification, Column column) {
    super(id, qualification, column.name());
    this.column = column;
  }

  public static AttributeDef build(int id, String tableAlias, Column c) {
    return new NativeAttributeDef(id, tableAlias, c);
  }

  @Override
  public boolean isNative() {
    return true;
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
  public void setReferences(List<AttributeDef> references) {
    throw new IllegalStateException("cannot call `setReference` on NativeAttributeDef");
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
  public ASTNode makeSelectItem() {
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, makeColumnRef());
    item.set(SELECT_ITEM_ALIAS, name());
    return item;
  }

  @Override
  public AttributeDef copyWithQualification(String qualification) {
    return new NativeAttributeDef(this.id(), qualification, referredColumn());
  }

  @Override
  public String toString() {
    return "%s AS %s.%s@%d".formatted(column, qualification(), name(), id());
  }
}
