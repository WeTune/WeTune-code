package sjtu.ipads.wtune.systhesis.exprlist;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.systhesis.operators.DropCountDistinct;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.AGGREGATE_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_SELECT_ITEMS;
import static sjtu.ipads.wtune.sqlparser.SQLNode.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class ReduceCountDistinct implements ExprListMutator {
  private final SQLNode target;

  public ReduceCountDistinct(SQLNode target) {
    this.target = target;
  }

  public static boolean canReduceCountDistinct(SQLNode node) {
    final SQLNode specNode = node.get(RESOLVED_QUERY_SCOPE).specNode();
    if (specNode == null) return false;
    for (SQLNode item : specNode.get(QUERY_SPEC_SELECT_ITEMS))
      if (item.get(SELECT_ITEM_EXPR).isFlagged(AGGREGATE_DISTINCT)) return true;
    return false;
  }

  @Override
  public SQLNode target() {
    return target;
  }

  @Override
  public SQLNode modifyAST(SQLNode root) {
    DropCountDistinct.build().apply(NodeFinder.find(root, target));
    return root;
  }
}
