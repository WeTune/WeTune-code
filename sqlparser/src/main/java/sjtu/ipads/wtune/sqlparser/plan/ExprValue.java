package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

class ExprValue implements Value {
  private String qualification;
  private String name;
  private final Expr expr;

  ExprValue(String name, Expr expr) {
    this.name = requireNonNull(simpleName(name));
    this.expr = requireNonNull(expr);
  }

  static ExprValue fromSelectItem(ASTNode selectItem) {
    if (WILDCARD.isInstance(selectItem))
      throw new IllegalArgumentException("ExprValue does not accept a wildcard");

    final String alias = selectItem.get(SELECT_ITEM_ALIAS);
    final ASTNode exprNode = selectItem.get(SELECT_ITEM_EXPR);

    // "" indicates an anonymous attribute.
    // Such an attribute can never be referenced by name in the query.
    // e.g., SELECT sub.* FROM (SELECT T.x + 1 FROM T) sub.
    // Here, "T.x+1" is anonymous. It can never be referenced elsewhere since it is unnamed.
    // However, it can still be included in the wildcard.

    final String name =
        alias != null
            ? alias
            : COLUMN_REF.isInstance(exprNode)
                ? exprNode.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN)
                : "__" + exprNode;
    final Expr expr = ExprImpl.mk(exprNode);

    return new ExprValue(name, expr);
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Column column() {
    return null;
  }

  @Override
  public Expr expr() {
    return expr;
  }

  @Override
  public String wildcardQualification() {
    return null;
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    if (qualification == null) return name;
    else return qualification + '.' + name;
  }
}
