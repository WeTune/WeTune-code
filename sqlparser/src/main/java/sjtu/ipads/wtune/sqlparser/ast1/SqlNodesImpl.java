package sjtu.ipads.wtune.sqlparser.ast1;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;

class SqlNodesImpl implements SqlNodes {
  private final SqlContext context;
  private final int[] nodeIds;

  protected SqlNodesImpl(SqlContext context, int[] nodeIds) {
    if (context == null && nodeIds != null) throw new IllegalArgumentException();

    this.context = context;
    this.nodeIds = coalesce(nodeIds, EMPTY_INT_ARRAY);
  }

  @Override
  public SqlNode get(int index) {
    return SqlNode.mk(context, nodeIds[index]);
  }

  @Override
  public List<SqlNode> asList() {
    return new DelegatedList();
  }

  @Override
  public int size() {
    return nodeIds.length;
  }

  public SqlContext context() {
    return context;
  }

  public int[] nodeIds() {
    return nodeIds;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final SqlNodesImpl that = (SqlNodesImpl) obj;
    return this.context == that.context && Arrays.equals(this.nodeIds, that.nodeIds);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(context) * 31 + Arrays.hashCode(nodeIds);
  }

  @Override
  public String toString() {
    return "SqlNodes{" + Arrays.toString(nodeIds) + "}";
  }

  private class DelegatedList extends AbstractList<SqlNode> {
    @Override
    public SqlNode get(int index) {
      return SqlNode.mk(context, nodeIds[index]);
    }

    @Override
    public int size() {
      return nodeIds.length;
    }
  }

  private static final int[] EMPTY_INT_ARRAY = new int[0];
}
