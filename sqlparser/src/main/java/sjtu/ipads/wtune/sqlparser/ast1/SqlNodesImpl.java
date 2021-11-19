package sjtu.ipads.wtune.sqlparser.ast1;

import gnu.trove.list.TIntList;
import sjtu.ipads.wtune.common.tree.LabeledTreeNodesBase;

class SqlNodesImpl extends LabeledTreeNodesBase<SqlKind, SqlContext, SqlNode> implements SqlNodes {
  protected SqlNodesImpl(SqlContext context) {
    super(context);
  }

  protected SqlNodesImpl(SqlContext context, TIntList nodeIds) {
    super(context, nodeIds);
  }

  @Override
  protected SqlNode mk(SqlContext context, int nodeId) {
    return SqlNode.mk(context, nodeId);
  }

  @Override
  public String toString() {
    return "SqlNodes{" + nodeIds() + "}";
  }
}
