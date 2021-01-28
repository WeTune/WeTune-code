package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;

import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_OFFSET;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.stmt.resolver.ParamManager.PARAM;

class ResolveParamSimple implements SQLVisitor {

  private static final Set<ExprType> INTERESTING_ENV =
      Set.of(UNARY, BINARY, TERNARY, TUPLE, ARRAY, MATCH);

  private int nextIndex;

  private void add(SQLNode node) {
    final ExprType exprType = node.parent().get(EXPR_KIND);
    if (exprType != null && INTERESTING_ENV.contains(exprType))
      node.set(PARAM, new Param(node, nextIndex++));
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    add(paramMarker);
    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    add(literal);
    return false;
  }

  @Override
  public boolean enterChild(SQLNode parent, FieldKey<SQLNode> key, SQLNode child) {
    if (key == QUERY_OFFSET) {
      if (child != null) add(child);
      return false;
    }

    return true;
  }

  public static ParamManager resolve(SQLNode node) {
    if (node.manager(ParamManager.class) == null)
      node.context().addManager(ParamManager.class, ParamManager.build());

    node.accept(new ResolveParamSimple());
    return node.manager(ParamManager.class);
  }
}