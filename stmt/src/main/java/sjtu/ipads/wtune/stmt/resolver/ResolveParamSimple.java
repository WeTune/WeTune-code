package sjtu.ipads.wtune.stmt.resolver;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_OFFSET;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.ARRAY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.MATCH;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.TERNARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.TUPLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.UNARY;
import static sjtu.ipads.wtune.stmt.resolver.ParamManager.PARAM;

import java.util.Set;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

class ResolveParamSimple implements ASTVistor {

  private static final Set<ExprKind> INTERESTING_ENV =
      Set.of(UNARY, BINARY, TERNARY, TUPLE, ARRAY, MATCH);

  private int nextIndex;

  private void add(ASTNode node) {
    final ExprKind exprKind = node.parent().get(EXPR_KIND);
    if (exprKind != null && INTERESTING_ENV.contains(exprKind))
      node.set(PARAM, new Param(node, nextIndex++));
  }

  @Override
  public boolean enterParamMarker(ASTNode paramMarker) {
    add(paramMarker);
    return false;
  }

  @Override
  public boolean enterLiteral(ASTNode literal) {
    add(literal);
    return false;
  }

  @Override
  public boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
    if (key == QUERY_OFFSET) {
      if (child != null) add(child);
      return false;
    }

    return true;
  }

  public static ParamManager resolve(ASTNode node) {
    if (node.manager(ParamManager.class) == null) node.context().addManager(ParamManager.build());

    node.accept(new ResolveParamSimple());
    return node.manager(ParamManager.class);
  }
}
