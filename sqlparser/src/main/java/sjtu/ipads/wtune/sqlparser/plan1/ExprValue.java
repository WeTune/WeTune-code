package sjtu.ipads.wtune.sqlparser.plan1;

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
  private final String name;
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

    final String name =
        alias != null
            ? alias
            : COLUMN_REF.isInstance(exprNode)
                ? exprNode.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN)
                : "";
    final Expr expr = ExprImpl.build(exprNode);

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
  public String toString() {
    if (qualification == null) return name;
    else return qualification + '.' + name;
  }
}
