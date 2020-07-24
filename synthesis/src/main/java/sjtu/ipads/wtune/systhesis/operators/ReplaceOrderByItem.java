package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;

import java.util.List;
import java.util.ListIterator;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_ORDER_BY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_CLAUSE_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;

public class ReplaceOrderByItem implements Operator {
  private final SQLNode target;
  private final SQLNode replacement;

  private ReplaceOrderByItem(SQLNode target, SQLNode replacement) {
    this.target = target;
    this.replacement = replacement;
  }

  public static ReplaceOrderByItem build(SQLNode target, SQLNode reference) {
    assert target != null;
    return new ReplaceOrderByItem(target, reference);
  }

  @Override
  public SQLNode apply(SQLNode node) {
    final SQLNode queryNode = node.get(RESOLVED_QUERY_SCOPE).queryNode();
    final List<SQLNode> items = queryNode.get(QUERY_ORDER_BY);
    final ListIterator<SQLNode> iter = items.listIterator();

    SQLNode target = this.target;
    boolean found = false;

    while (iter.hasNext()) {
      final SQLNode item = iter.next();
      if (!nodeEquals(item, target)) continue;
      found = true;
      target = item;
      break;
    }

    assert found;
    if (replacement == null) {
      iter.remove();
      if (items.isEmpty()) queryNode.remove(QUERY_ORDER_BY);
      return node;
    }

    Operator.replaceNode(target, replacement);

    return node;
  }
}
