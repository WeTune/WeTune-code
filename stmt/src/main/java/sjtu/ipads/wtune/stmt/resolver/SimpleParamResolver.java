package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.Param;
import sjtu.ipads.wtune.stmt.attrs.ParamModifier;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_LIMIT;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_OFFSET;
import static sjtu.ipads.wtune.stmt.attrs.ParamModifier.Type.GEN_OFFSET;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.PARAM_INDEX;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_PARAM;

public class SimpleParamResolver implements SQLVisitor, Resolver {
  private int maxIndex = -1;
  private boolean firstPass = true;

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    markParam(paramMarker, true);
    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    markParam(literal, true);
    return false;
  }

  @Override
  public boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    if (key == QUERY_OFFSET) {
      if (child != null) markParam(child, false);
      return false;
    }

    return true;
  }

  private static final Set<SQLExpr.Kind> VALID_ENV =
      Set.of(
          SQLExpr.Kind.UNARY,
          SQLExpr.Kind.BINARY,
          SQLExpr.Kind.TERNARY,
          SQLExpr.Kind.TUPLE,
          SQLExpr.Kind.ARRAY,
          SQLExpr.Kind.MATCH);

  private void markParam(SQLNode node, boolean checkParent) {
    final SQLExpr.Kind parentKind = exprKind(node.parent());
    if (checkParent && (parentKind == null || !VALID_ENV.contains(parentKind))) return;

    final Integer index = node.get(PARAM_INDEX);
    if (index != null && firstPass) maxIndex = Math.max(maxIndex, index);
    if (index == null && !firstPass) node.put(PARAM_INDEX, ++maxIndex);
  }

  @Override
  public boolean resolve(Statement stmt, SQLNode node) {
    final SimpleParamResolver resolver = new SimpleParamResolver();
    resolver.firstPass = true;
    node.accept(resolver);
    resolver.firstPass = false;
    node.accept(resolver);
    return true;
  }
}
