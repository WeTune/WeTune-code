package sjtu.ipads.wtune.sql.support;

import gnu.trove.list.TIntList;
import sjtu.ipads.wtune.sql.ast1.AdditionalInfo;
import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;

import java.util.AbstractList;
import java.util.List;

public class RenumberListener extends AbstractList<SqlNode>
    implements AdditionalInfo<RenumberListener>,
        AdditionalInfo.Key<RenumberListener>,
        List<SqlNode>,
        AutoCloseable {
  private final SqlContext ctx;
  private TIntList list;

  public RenumberListener(SqlContext ctx) {
    this.ctx = ctx;
  }

  public void watch(TIntList list) {
    this.list = list;
  }

  public static RenumberListener watch(SqlContext sql, TIntList list) {
    final RenumberListener listener = new RenumberListener(sql);
    listener.watch(list);
    sql.getAdditionalInfo(listener);
    return listener;
  }

  public static RenumberListener watch(SqlContext sql, SqlNodes list) {
    return watch(sql, list.nodeIds());
  }

  @Override
  public void renumberNode(int oldId, int newId) {
    for (int i = 0, bound = list.size(); i < bound; ++i) {
      if (list.get(i) == oldId) list.set(i, newId);
    }
  }

  @Override
  public void deleteNode(int nodeId) {
    list.remove(nodeId);
  }

  @Override
  public RenumberListener init(SqlContext sql) {
    return new RenumberListener(sql);
  }

  @Override
  public SqlNode get(int index) {
    return SqlNode.mk(ctx, list.get(index));
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public void close() {
    ctx.removeAdditionalInfo(this);
  }
}
