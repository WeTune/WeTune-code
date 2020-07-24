package sjtu.ipads.wtune.systhesis;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLFormatter;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.SelectItem;
import sjtu.ipads.wtune.stmt.schema.Column;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class TemplatizeSQLFormatter extends SQLFormatter {
  private final boolean templatizeColumnRef;

  public TemplatizeSQLFormatter(boolean templatizeColumnRef) {
    this.templatizeColumnRef = templatizeColumnRef;
  }

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    if (!templatizeColumnRef) return super.enterColumnRef(columnRef);
    final ColumnRef cRef = columnRef.get(RESOLVED_COLUMN_REF);
    if (cRef == null) return super.enterColumnRef(columnRef);
    final Column column = cRef.resolveAsColumn();
    append(column == null ? "<??>" : column.dataType().category());

    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    append("?");
    return false;
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    append("?");
    return false;
  }

  @Override
  public boolean enterUnary(SQLNode unary) {
    final SQLExpr.UnaryOp op = unary.get(UNARY_OP);
    if (op == SQLExpr.UnaryOp.UNARY_MINUS || op == SQLExpr.UnaryOp.UNARY_PLUS)
      if (exprKind(unary.get(UNARY_EXPR)) == Kind.LITERAL) {
        append("?");
        return false;
      }

    return super.enterUnary(unary);
  }

  public static String templatize(SQLNode node) {
    return templatize(node, false);
  }

  public static String templatize(SQLNode node, boolean templatizeColumnRef) {
    return node.toString(new TemplatizeSQLFormatter(templatizeColumnRef));
  }
}
