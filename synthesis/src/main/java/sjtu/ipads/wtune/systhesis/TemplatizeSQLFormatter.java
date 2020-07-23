package sjtu.ipads.wtune.systhesis;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLFormatter;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;

public class TemplatizeSQLFormatter extends SQLFormatter {
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
    return node.toString(new TemplatizeSQLFormatter());
  }
}
