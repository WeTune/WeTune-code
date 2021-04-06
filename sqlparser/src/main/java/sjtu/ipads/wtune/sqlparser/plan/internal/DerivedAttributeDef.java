package sjtu.ipads.wtune.sqlparser.plan.internal;

import static java.util.Collections.unmodifiableList;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

import java.util.Collections;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.schema.Column;

public class DerivedAttributeDef extends AttributeDefBase {
  private final ASTNode expr;
  private final boolean isIdentity;

  private List<AttributeDef> references;

  public DerivedAttributeDef(
      int id, String qualification, String name, ASTNode expr, List<AttributeDef> references) {
    super(id, qualification, name);
    this.expr = expr;
    this.isIdentity = COLUMN_REF.isInstance(expr);
    this.references = references;
  }

  public static AttributeDef build(int id, String qualification, String name, ASTNode expr) {
    return new DerivedAttributeDef(id, qualification, name, expr, null);
  }

  public ASTNode expr() {
    return expr;
  }

  @Override
  public boolean isIdentity() {
    return isIdentity;
  }

  @Override
  public boolean isNative() {
    return false;
  }

  @Override
  public List<AttributeDef> references() {
    return references == null ? null : unmodifiableList(references);
  }

  @Override
  public AttributeDef upstream() {
    if (references == null)
      throw new IllegalStateException(
          "cannot call `upstream` on DerivedAttributeDef before `setReferences` are called");

    if (isIdentity()) return references.get(0);
    else return null;
  }

  @Override
  public AttributeDef source() {
    final AttributeDef upstream = upstream();
    if (upstream == null) return this;
    else return upstream.source();
  }

  @Override
  public Column referredColumn() {
    final AttributeDef src = nativeSource();
    assert src == null || src instanceof NativeAttributeDef;
    return src != null ? src.referredColumn() : null;
  }

  @Override
  public void setReferences(List<AttributeDef> references) {
    this.references = references;
  }

  @Override
  public boolean referencesTo(String qualification, String alias) {
    if ((qualification == null || qualification.equals(this.qualification()))
        && alias.equals(this.name())) return true;
    final AttributeDef upstream = upstream();
    return upstream != null && upstream.referencesTo(qualification, alias);
  }

  @Override
  public boolean referencesTo(int id) {
    return this.id() == id || (isIdentity() && upstream().referencesTo(id));
  }

  @Override
  public AttributeDef copyWithQualification(String qualification) {
    return new DerivedAttributeDef(id(), qualification, name(), expr, references);
  }

  @Override
  public ASTNode makeSelectItem() {
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, expr.deepCopy());
    item.set(SELECT_ITEM_ALIAS, name());
    return item;
  }

  @Override
  public String toString() {
    return "%s AS %s.%s@%d".formatted(expr, qualification(), name(), id());
  }
}
