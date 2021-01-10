package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.stmt.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.QUERY_OFFSET;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.PARAM_INDEX;

class ResolveParamSimple {
  private static class ParamCollector implements SQLVisitor {
    private static final Set<ExprType> INTERESTING_ENV =
        Set.of(UNARY, BINARY, TERNARY, TUPLE, ARRAY, MATCH);
    private final List<SQLNode> nodes = new ArrayList<>();

    private void add(SQLNode node) {
      if (INTERESTING_ENV.contains(node.parent().get(EXPR_KIND))) nodes.add(node);
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
    public boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
      if (key == QUERY_OFFSET) {
        if (child != null) add(child);
        return false;
      }

      return true;
    }
  }

  public static void resolve(Statement stmt) {
    final ParamCollector collector = new ParamCollector();
    stmt.parsed().accept(collector);

    final List<SQLNode> nodes = collector.nodes;
    for (int i = 0; i < nodes.size(); i++) nodes.get(i).put(PARAM_INDEX, i);
  }
}
