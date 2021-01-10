package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.stmt.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.COLUMN_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;
import static sjtu.ipads.wtune.stmt.attrs.SelectItem.fromNode;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

/**
 * Resolve output column (i.e. select item) of query scope, meanwhile expand wildcard expression.
 */
class ResolveSelection implements SQLVisitor {
  private static final System.Logger LOG = System.getLogger("Stmt.Resolver.Selection");

  private final Statement stmt;

  public ResolveSelection(Statement stmt) {
    this.stmt = stmt;
  }

  public static void resolve(Statement stmt) {
    stmt.parsed().accept(new ResolveSelection(stmt));
  }

  @Override
  public void leaveQuerySpec(SQLNode querySpec) {
    final QueryScope scope = querySpec.get(RESOLVED_QUERY_SCOPE);

    final List<SQLNode> selectItems = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final List<SQLNode> modifiedItems = expandWildcardIfNeed(selectItems);

    querySpec.put(QUERY_SPEC_SELECT_ITEMS, modifiedItems);

    for (SQLNode item : modifiedItems) {
      addAlias(item);
      scope.setScope(item);
      scope.addSelectItem(fromNode(item));
    }
  }

  private List<SQLNode> expandWildcardIfNeed(List<SQLNode> selectItems) {
    if (selectItems.stream().noneMatch(it -> WILDCARD.isInstance(it.get(SELECT_ITEM_EXPR))))
      return selectItems;

    final List<SQLNode> modifiedSelectItems = new ArrayList<>(selectItems.size());

    for (SQLNode itemNode : selectItems) {
      final SQLNode expr = itemNode.get(SELECT_ITEM_EXPR);
      if (WILDCARD.isInstance(expr)) modifiedSelectItems.addAll(expandWildcard(expr));
      else modifiedSelectItems.add(itemNode);
    }

    return modifiedSelectItems;
  }

  private static void addAlias(SQLNode selectItem) {
    final SQLNode expr = selectItem.get(SELECT_ITEM_EXPR);
    if (COLUMN_REF.isInstance(expr) && selectItem.get(SELECT_ITEM_ALIAS) == null)
      selectItem.put(SELECT_ITEM_ALIAS, expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN));
  }

  private List<SQLNode> expandWildcard(SQLNode node) {
    assert WILDCARD.isInstance(node);
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    final SQLNode tableId = node.get(WILDCARD_TABLE);

    if (tableId != null) {
      // qualified
      final String tableName = tableId.get(TABLE_NAME_TABLE);
      final TableSource tableSource = scope.resolveTable(tableName);
      if (tableSource != null) return selectItemsFrom(scope, tableSource);

      LOG.log(
          WARNING,
          "unresolved wildcard selection {3}\n{0}\n{1}",
          stmt,
          stmt.parsed().toString(false),
          node);
      return Collections.emptyList();

    } else
      // unqualified
      return scope.tableSources().values().stream()
          .map(it -> selectItemsFrom(scope, it))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
  }

  private static List<SQLNode> selectItemsFrom(QueryScope scope, TableSource tableSource) {
    final List<String> selections = tableSource.namedSelections();
    final List<SQLNode> items = new ArrayList<>(selections.size());
    for (String selection : selections) {
      final SQLNode columnName = SQLNode.simple(COLUMN_NAME);
      columnName.put(COLUMN_NAME_TABLE, tableSource.name());
      columnName.put(COLUMN_NAME_COLUMN, selection);

      final SQLNode columnRef = SQLNode.simple(COLUMN_REF);
      columnRef.put(COLUMN_REF_COLUMN, columnName);

      final SQLNode selectItem = SQLNode.simple(SELECT_ITEM);
      selectItem.put(SELECT_ITEM_EXPR, columnRef);

      items.add(selectItem);
    }
    items.forEach(scope::setScope);
    return items;
  }
}
