package sjtu.ipads.wtune.sql.support.locator;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlVisitor;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.Case_Cond;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.When_Cond;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.Query;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.QuerySpec_Having;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.QuerySpec_Where;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.Joined_On;

class PredicateLocator implements SqlVisitor, SqlGatherer, SqlFinder {
  private final TIntList nodes;
  private final boolean scoped;
  private final boolean bottomUp;
  private int exemptQueryNode;

  protected PredicateLocator(boolean scoped, boolean bottomUp, int expectedNumNodes) {
    this.nodes = expectedNumNodes >= 0 ? new TIntArrayList(expectedNumNodes) : new TIntArrayList();
    this.bottomUp = bottomUp;
    this.scoped = scoped;
  }

  @Override
  public int find(SqlNode root) {
    exemptQueryNode = Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
    root.accept(this);
    return nodes.isEmpty() ? NO_SUCH_NODE : nodes.get(0);
  }

  @Override
  public TIntList gather(SqlNode root) {
    exemptQueryNode = Query.isInstance(root) ? root.nodeId() : NO_SUCH_NODE;
    root.accept(this);
    return nodes;
  }

  @Override
  public TIntList nodeIds() {
    return nodes;
  }

  @Override
  public boolean enterQuery(SqlNode query) {
    return !scoped || query.nodeId() == exemptQueryNode;
  }

  @Override
  public boolean enterCase(SqlNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.$(Case_Cond) == null;
  }

  @Override
  public boolean enterWhen(SqlNode when) {
    if (!bottomUp && when != null) nodes.add(when.$(When_Cond).nodeId());
    return false;
  }

  @Override
  public boolean enterChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (!bottomUp
        && child != null
        && (key == Joined_On || key == QuerySpec_Where || key == QuerySpec_Having)) {
      nodes.add(child.nodeId());
    }
    return true;
  }

  @Override
  public void leaveChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    if (bottomUp
        && child != null
        && (key == Joined_On || key == QuerySpec_Where || key == QuerySpec_Having)) {
      nodes.add(child.nodeId());
    }
  }

  @Override
  public void leaveWhen(SqlNode when) {
    if (bottomUp && when != null) nodes.add(when.$(When_Cond).nodeId());
  }
}
