package sjtu.ipads.wtune.sql.support;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.ast1.SqlVisitor;

import static sjtu.ipads.wtune.sql.ast1.ExprFields.Case_Cond;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.When_Cond;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.QuerySpec_Having;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.QuerySpec_Where;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.Joined_On;

public class BoolContextCollector implements SqlVisitor {
  private final TIntList nodes;

  protected BoolContextCollector() {
    this.nodes = new TIntArrayList();
  }

  protected BoolContextCollector(int expectedNumNodes) {
    this.nodes = new TIntArrayList(expectedNumNodes);
  }

  @Override
  public boolean enterCase(SqlNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.$(Case_Cond) == null;
  }

  @Override
  public boolean enterWhen(SqlNode when) {
    if (when != null) nodes.add(when.$(When_Cond).nodeId());
    return false;
  }

  @Override
  public boolean enterChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (child != null && (key == Joined_On || key == QuerySpec_Where || key == QuerySpec_Having)) {
      nodes.add(child.nodeId());
    }
    return true;
  }

  public static SqlNodes collect(SqlNode root) {
    final BoolContextCollector collector = new BoolContextCollector();
    root.accept(collector);
    return SqlNodes.mk(root.context(), collector.nodes);
  }
}
