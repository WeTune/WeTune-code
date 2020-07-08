package sjtu.ipads.wtune.systhesis.exprlist;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.systhesis.operators.DropDistinct;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_DISTINCT;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class ReduceDistinct implements ExprListMutator {
  private final SQLNode target;

  public ReduceDistinct(SQLNode target) {
    assert target != null;
    this.target = target;
  }

  public static boolean canReduceDistinct(SQLNode node) {
    final SQLNode specNode = node.get(RESOLVED_QUERY_SCOPE).specNode();
    return specNode != null && specNode.isFlagged(QUERY_SPEC_DISTINCT);
  }

  @Override
  public SQLNode target() {
    return target;
  }

  @Override
  public SQLNode modifyAST(SQLNode root) {
    DropDistinct.build().apply(NodeFinder.find(root, target));
    return root;
  }
}
