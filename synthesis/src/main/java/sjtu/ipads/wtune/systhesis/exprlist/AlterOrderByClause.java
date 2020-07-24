package sjtu.ipads.wtune.systhesis.exprlist;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.attrs.SelectItem;
import sjtu.ipads.wtune.stmt.similarity.output.OutputSimKey;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.Operator;
import sjtu.ipads.wtune.systhesis.operators.ReplaceOrderByItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ORDER_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_ORDER_BY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class AlterOrderByClause implements ExprListMutator {
  private final List<Operator> operators;

  public AlterOrderByClause(List<Operator> operators) {
    this.operators = operators;
  }

  public static AlterOrderByClause build(Statement base, Statement ref) {
    final SQLNode baseRoot = base.parsed();
    final SQLNode refRoot = ref.parsed();

    // output keys
    final OutputSimKey[] baseKeys = base.keys();
    final OutputSimKey[] refKeys = ref.keys();

    if (isEmptyResult(baseKeys) || isEmptyResult(refKeys)) return null;

    // ORDER BY clause
    final List<SQLNode> baseOrderItems = baseRoot.get(QUERY_ORDER_BY);
    final List<SQLNode> refOrderItems = refRoot.get(QUERY_ORDER_BY);

    if (baseOrderItems == null) return null;

    // select items
    final List<SelectItem> baseSelectItems = baseRoot.get(RESOLVED_QUERY_SCOPE).selectItems();
    final List<SelectItem> refSelectItems = refRoot.get(RESOLVED_QUERY_SCOPE).selectItems();

    // index of selection items that originate from ORDER BY
    final List<Integer> baseOrderByItemIndexes = indexOfOrderByItems(baseSelectItems);
    final List<Integer> refOrderByItemIndexes = indexOfOrderByItems(refSelectItems);

    final List<Operator> operators = new ArrayList<>();
    final Set<Integer> modifiedOrderItemIndexes = new HashSet<>();

    // for each order-by item in base stmt
    for (Integer baseOrderByItemIndex : baseOrderByItemIndexes) {
      final int baseOrderClauseIndex =
          indexInOrderByClause(baseSelectItems.get(baseOrderByItemIndex));
      //// avoid duplicate examine
      if (!modifiedOrderItemIndexes.add(baseOrderClauseIndex)) continue;

      final SQLNode baseOrderItem = baseOrderItems.get(baseOrderClauseIndex);
      final boolean isPlain =
          exprKind(baseOrderItem.get(ORDER_ITEM_EXPR)) == SQLExpr.Kind.COLUMN_REF;

      final OutputSimKey baseKey = baseKeys[baseOrderByItemIndex];
      if (baseKey.isEmpty()) continue;

      // find a matched column in ref
      final List<Integer> matched = new ArrayList<>();
      for (Integer refOrderByItemIndex : refOrderByItemIndexes)
        if (baseKey.equals(refKeys[refOrderByItemIndex])) matched.add(refOrderByItemIndex);

      // no such match found, so it can be removed
      if (matched.isEmpty()) {
        operators.add(ReplaceOrderByItem.build(baseOrderItem, null)); // remove
        continue;
      }

      // no modification can be applied to a plain (e.g. a simple column ref expr) so just skip
      if (isPlain) continue;

      // pick the best one in matches as the replacement.
      // simple column ref is the best as long as the count of used columns is identical
      // otherwise just pick a random one (very rare case, if ever happens)
      final int baseColumnRefCount = ColumnRefCollector.collect(baseOrderItem).size();
      SQLNode bestReplacement = null;

      for (Integer refOrderClauseIndex : matched) {
        final SQLNode refOrderItem = refOrderItems.get(refOrderClauseIndex);
        if (baseColumnRefCount == 1 // fast-path, consider plain column ref as best replacement
            && exprKind(refOrderItem.get(ORDER_ITEM_EXPR)) == SQLExpr.Kind.COLUMN_REF) {
          bestReplacement = refOrderItem;
          break;
        }
        final int refColumnRefCount = ColumnRefCollector.collect(refOrderItem).size();
        if (refColumnRefCount == baseColumnRefCount) bestReplacement = refOrderItem;
      }

      // no such best replacement, just skip
      if (bestReplacement == null) continue;

      operators.add(ReplaceOrderByItem.build(baseOrderItem, bestReplacement.copy()));
    }

    return new AlterOrderByClause(operators);
  }

  private static boolean isEmptyResult(OutputSimKey[] keys) {
    for (OutputSimKey key : keys) if (!key.isEmpty()) return false;
    return true;
  }

  private static List<Integer> indexOfOrderByItems(List<SelectItem> items) {
    final List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) if (isOrderByItem(items.get(i))) indexes.add(i);
    return indexes;
  }

  private static boolean isOrderByItem(SelectItem item) {
    final String alias = item.alias();
    return alias != null && alias.startsWith("_orderby");
  }

  private static int indexInOrderByClause(SelectItem item) {
    final String alias = item.alias();
    final int start = alias.indexOf('_', alias.indexOf('_') + 1) + 1;
    final int end = alias.indexOf('_', start);
    assert start > 0 && start < alias.length();
    assert end > 0 && end < alias.length();
    return Integer.parseInt(alias.substring(start, end));
  }

  @Override
  public SQLNode target() {
    return null;
  }

  @Override
  public SQLNode modifyAST(SQLNode root) {
    for (Operator operator : operators) operator.apply(root);
    return root;
  }
}
